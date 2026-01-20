import React, { useEffect, useState } from 'react';
import { api } from '../api.js';
import SectionHeader from '../components/SectionHeader.jsx';
import Toast from '../components/Toast.jsx';

export default function Dashboard(){
  const [health, setHealth] = useState(null);
  const [toast, setToast] = useState(null);

  useEffect(()=>{
    (async ()=>{
      try{
        // If actuator exists, this will work; otherwise ignore.
        const h = await api.get('/actuator/health');
        setHealth(h);
      }catch(e){
        setToast({kind:'err', title:'Health endpoint not reachable', detail:'If you don\'t use Spring Actuator, ignore this. Try Quick API instead.'});
      }
    })();
  },[]);

  return (
    <>
      <SectionHeader
        title="Dashboard"
        right={<a className="btn" href="#" onClick={(e)=>{e.preventDefault(); window.open('/swagger-ui/index.html','_blank');}}>Swagger (varsa)</a>}
      />
      <div className="card">
        <div className="h1" style={{fontSize:16}}>Connection</div>
        <div className="p">API Base: <span className="mono">{api.baseUrl()}</span> (Nginx proxy /api → backend)</div>
        <div className="hr" />
        <div className="h1" style={{fontSize:16}}>Health</div>
        {health ? (
          <pre className="mono" style={{fontSize:12, whiteSpace:'pre-wrap'}}>{JSON.stringify(health, null, 2)}</pre>
        ) : (
          <div className="small">No data yet.</div>
        )}
        <Toast kind={toast?.kind} title={toast?.title} detail={toast?.detail} />
      </div>
    </>
  );
}
