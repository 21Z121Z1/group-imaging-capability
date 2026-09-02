from pathlib import Path
import re,sys
root=Path(__file__).resolve().parents[1]; ps=sorted(root.glob('[0-9][0-9]-*')); assert len(ps)==9
ports=set(); fports=set(); themes=[]
for p in ps:
    for req in ['README.md','backend/pom.xml','backend/src/main/resources/application.yml','frontend/package.json','frontend/src/App.vue','frontend/src/style.css']:
        assert (p/req).exists(), f'missing {p.name}/{req}'
    y=(p/'backend/src/main/resources/application.yml').read_text(); m=re.search(r'port:\s*(81\d\d)',y); assert m; assert m.group(1) not in ports;ports.add(m.group(1))
    pkg=(p/'frontend/package.json').read_text(); fm=re.search(r'vite --port (51\d\d)',pkg);assert fm;assert fm.group(1) not in fports;fports.add(fm.group(1))
    css=(p/'frontend/src/style.css').read_text();themes.append(css)
    for j in p.glob('backend/src/main/java/**/*.java'):
        t=j.read_text();assert 'GENERATED_COPY' not in t, j
        # rough lexical balance after stripping string literals and comments
        x=re.sub(r'"(?:\\.|[^"\\])*"','""',t); x=re.sub(r'//.*','',x); assert x.count('{')==x.count('}'),f'brace mismatch {j}'
assert len({hash(x) for x in themes})==9
print('STATIC VALIDATION PASSED: 9 projects, unique ports/themes, required files, no placeholders, balanced Java braces')
