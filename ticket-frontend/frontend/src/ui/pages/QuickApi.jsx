import React, { useMemo, useState } from 'react';
import { api } from '../api.js';
import JsonBox from '../components/JsonBox.jsx';
import Toast from '../components/Toast.jsx';
import SectionHeader from '../components/SectionHeader.jsx';

function pretty(obj){
  try{
    return typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2);
  }catch{
    return String(obj);
  }
}

export default function QuickApi(){
  const [method, setMethod] = useState('GET');
  const [path, setPath] = useState('/events');
  const [reqBody, setReqBody] = useState('');
  const [resBody, setResBody] = useState('');
  const [toast, setToast] = useState(null);

  const examples = useMemo(()=>({
    "List events": { m: 'GET', p: '/events?page=0&size=20' },
    "Get event": { m: 'GET', p: '/events/1' },
    "Create event": { m: 'POST', p: '/events', b: {
      type: 'CONCERT',
      name: 'Mgmeral Live Concert',
      summary: 'Concert summary',
      description: 'Demo concert event',
      startDate: '2026-02-01T18:00:00Z',
      endDate: '2026-02-01T23:00:00Z'
    } },
    "Create seance": { m: 'POST', p: '/events/1/seances', b: {
      startDateTime: '2026-02-01T18:00:00Z',
      capacity: 500
    } },
    "Availability": { m: 'GET', p: '/seances/1/availability' },
    "Create hold": { m: 'POST', p: '/holds', b: {
      userId: 123,
      seanceId: 1,
      quantity: 2,
      idempotencyKey: 'demo-key-1'
    } },
    "Release hold": { m: 'DELETE', p: '/holds/1' },
    "Purchase": { m: 'POST', p: '/purchases', b: {
      holdId: 1,
      paymentRef: 'PAY-REF-001',
      idempotencyKey: 'purchase-key-1'
    } },
  }),[]);

  async function send(){
    setToast(null);
    setResBody('');
    try{
      let body;
      if(method !== 'GET' && method !== 'DELETE'){
        body = reqBody?.trim() ? JSON.parse(reqBody) : {};
      }
      const data = method === 'GET'
        ? await api.get(path)
        : method === 'POST'
          ? await api.post(path, body)
          : method === 'PUT'
            ? await api.put(path, body)
            : await api.del(path);
      setResBody(pretty(data));
      setToast({kind:'ok', title:'OK', detail:'Request completed'});
    }catch(e){
      setToast({kind:'err', title:'Request failed', detail: e?.message || String(e)});
      setResBody(pretty(e?.payload ?? ''));
    }
  }

  function loadExample(key){
    const ex = examples[key];
    setMethod(ex.m);
    setPath(ex.p);
    setReqBody(ex.b ? JSON.stringify(ex.b, null, 2) : '');
    setResBody('');
    setToast(null);
  }

  return (
    <>
      <SectionHeader
        title="Quick API"
        desc={"Backend endpointlerini bilmiyorsak bile iş görüyor: method + path + JSON body. Varsayılan olarak Nginx /api üzerinden backend'e proxy'ler."}
        right={<div className="small">Base: <span className="mono">{api.baseUrl()}</span></div>}
      />

      <div className="card">
        <div className="row" style={{marginBottom:10}}>
          <div className="field" style={{flex:'0 0 140px', minWidth:140}}>
            <div className="label">Method</div>
            <select className="select" value={method} onChange={e=>setMethod(e.target.value)}>
              <option>GET</option>
              <option>POST</option>
              <option>PUT</option>
              <option>DELETE</option>
            </select>
          </div>
          <div className="field" style={{flex:'1 1 500px'}}>
            <div className="label">Path</div>
            <input className="input" value={path} onChange={e=>setPath(e.target.value)} placeholder="/events?page=0&size=20" />
          </div>
          <button className="btn primary" onClick={send}>Send</button>
        </div>

        <div className="row" style={{gap:8, marginBottom:10}}>
          {Object.keys(examples).map(k => (
            <button key={k} className="btn" onClick={()=>loadExample(k)}>{k}</button>
          ))}
        </div>

        <div className="grid">
          <JsonBox
            label={method === 'GET' || method === 'DELETE' ? 'Body (disabled)' : 'Request JSON'}
            value={reqBody}
            onChange={setReqBody}
            rows={14}
            placeholder={method === 'GET' || method === 'DELETE' ? '' : '{\n  "example": true\n}'}
          />
          <JsonBox
            label="Response"
            value={resBody}
            onChange={()=>{}}
            rows={14}
          />
        </div>

        <Toast kind={toast?.kind} title={toast?.title} detail={toast?.detail} />
      </div>

      <div style={{height:12}} />

      <div className="card">
        <div className="h1" style={{fontSize:16}}>Beklenen endpoint seti</div>
        <div className="p" style={{marginTop:6}}>
          PDF case'e göre (milestone 1–4):
          <div className="hr" />
          <div className="kv">
            <div>Events</div><div>POST/PUT/GET/DELETE <span className="mono">/events</span>, list: <span className="mono">GET /events</span></div>
            <div>Performers</div><div>Benzer CRUD (projende farklı olabilir)</div>
            <div>Seances</div><div><span className="mono">POST /events/{{eventId}}/seances</span>, <span className="mono">GET /seances/{{id}}</span>, <span className="mono">GET /seances?eventId=&dateFrom=&dateTo=</span></div>
            <div>Availability</div><div><span className="mono">GET /seances/{{id}}/availability</span></div>
            <div>Holds</div><div><span className="mono">POST /holds</span>, <span className="mono">GET /holds/{{holdId}}</span>, <span className="mono">DELETE /holds/{{holdId}}</span></div>
            <div>Purchase</div><div><span className="mono">POST /purchases</span> (+ idempotency)</div>
            <div>Payment</div><div><span className="mono">POST /payments/authorize</span> (mock)</div>
          </div>
        </div>
      </div>
    </>
  );
}
