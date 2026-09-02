let token = '';
export const session={ get token(){return token}, set token(v){token=v||''} };
export async function api(path, options={}){ const headers={...(options.headers||{}),'Content-Type':'application/json'}; if(session.token) headers.Authorization=`Bearer ${session.token}`; const r=await fetch(path,{...options,headers}); if(r.status===204)return null; const text=await r.text(); const data=text?JSON.parse(text):null; if(!r.ok) throw new Error(data?.message||`HTTP ${r.status}`); return data; }
