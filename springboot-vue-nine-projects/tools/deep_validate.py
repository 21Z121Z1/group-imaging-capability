from pathlib import Path
import re, json, xml.etree.ElementTree as ET, sys
root=Path('/mnt/data/springboot-vue-nine-projects')
errors=[]
for proj in sorted(root.glob('[0-9][0-9]-*')):
    # XML + JSON parse
    try: ET.parse(proj/'backend/pom.xml')
    except Exception as e: errors.append(f'{proj.name}: pom xml {e}')
    try: json.loads((proj/'frontend/package.json').read_text())
    except Exception as e: errors.append(f'{proj.name}: package json {e}')
    # java package/path and project-local imports
    files=list(proj.glob('backend/src/main/java/**/*.java'))+list(proj.glob('backend/src/test/java/**/*.java'))
    symbols=set()
    infos=[]
    for f in files:
        t=f.read_text()
        pm=re.search(r'\bpackage\s+([\w.]+)\s*;',t)
        if not pm: errors.append(f'{f}: no package'); continue
        pkg=pm.group(1)
        sm=re.search(r'\b(?:public\s+)?(?:class|interface|record|enum)\s+(\w+)',t)
        if sm: symbols.add(pkg+'.'+sm.group(1))
        infos.append((f,t,pkg))
        expected=Path(*pkg.split('.'))/f.name
        if not str(f).endswith(str(expected)): errors.append(f'{f}: package path mismatch {pkg}')
        # lexical basic
        x=re.sub(r'/\*.*?\*/','',t,flags=re.S);x=re.sub(r'"(?:\\.|[^"\\])*"','""',x);x=re.sub(r'//.*','',x)
        for a,b,ch in [('(',')','paren'),('[',']','bracket'),('{','}','brace')]:
            if x.count(a)!=x.count(b): errors.append(f'{f}: {ch} mismatch')
    base_prefix=next((pkg.rsplit('.',1)[0] for _,_,pkg in infos if pkg.endswith('.common')),None)
    if base_prefix:
        for f,t,pkg in infos:
            for imp in re.findall(r'\bimport\s+([\w.]+)\s*;',t):
                if imp.startswith(base_prefix+'.') and imp not in symbols and not imp.endswith('.*'):
                    errors.append(f'{f}: unresolved project import {imp}')
    # security checks
    for f in proj.glob('backend/src/main/java/**/controller/*.java'): pass
    for f in proj.glob('backend/src/main/java/**/*.java'):
        t=f.read_text()
        if 'UserAdminController' in f.name and 'getPasswordHash' in t: errors.append(f'{f}: leaks password hash')
        if 'ProjectDataSeed' in f.name and '@Order(10)' not in t: errors.append(f'{f}: seed order missing')
        if 'AuthSeed' in f.name and '@Order(0)' not in t: errors.append(f'{f}: auth seed order missing')
    # frontend SFC and quick endpoint presence
    app=(proj/'frontend/src/App.vue').read_text(); css=(proj/'frontend/src/style.css').read_text()
    if app.count('<template>')<1 or app.count('</template>')<1 or app.count('<script setup>')!=1 or app.count('</script>')!=1: errors.append(f'{proj.name}: malformed App.vue blocks')
    if css.count('{')!=css.count('}'): errors.append(f'{proj.name}: css brace mismatch')
    endpoints=set()
    for f in proj.glob('backend/src/main/java/**/*.java'):
        t=f.read_text(); base=''
        m=re.search(r'@RequestMapping\("([^"]+)"\)',t)
        if m: base=m.group(1); endpoints.add(base)
    for q in re.findall(r"api\('(/api/[^']+)'",app):
        base='/'+'/'.join(q.strip('/').split('/')[:2])
        if base not in endpoints and not base.startswith('/api/auth'):
            errors.append(f'{proj.name}: frontend endpoint {q} no controller base {base}; have {sorted(endpoints)}')
# specific invariants
lab=root/'09-lab-reservation'
lf=next(lab.glob('backend/src/main/java/**/domain/LaboratoryRepository.java')).read_text()
lc=next(lab.glob('backend/src/main/java/**/domain/LabReservationController.java')).read_text()
if 'PESSIMISTIC_WRITE' not in lf or '@Transactional' not in lc or 'findLockedById' not in lc: errors.append('lab reservation not serialized')
for slug,name in [('01-campus-club','Membership.java'),('04-homework-management','Submission.java'),('05-lost-found','ClaimRequest.java')]:
    t=next((root/slug).glob(f'backend/src/main/java/**/domain/{name}')).read_text()
    if 'uniqueConstraints' not in t: errors.append(f'{slug}: missing unique constraint {name}')
if errors:
    print('\n'.join('ERROR '+e for e in errors));sys.exit(1)
print('DEEP STATIC VALIDATION PASSED: XML/JSON, package paths/imports, lexical balance, seed order, security DTO, frontend endpoints, concurrency invariants')
