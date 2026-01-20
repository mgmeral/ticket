import React, { useState } from 'react';
import SectionHeader from '../components/SectionHeader.jsx';
import JsonBox from '../components/JsonBox.jsx';
import Toast from '../components/Toast.jsx';
import { api } from '../api.js';

function pretty(obj){
  try{ return typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2); }
  catch{ return String(obj); }
}

function EntityConsole({
  title,
  desc,
  listPath,
  getPath,
  createPath,
  updatePath,
  deletePath,
  defaultCreate,
  defaultUpdate,
}){
  const [listQuery, setListQuery] = useState('');
  const [entityId, setEntityId] = useState('1');
  const [createJson, setCreateJson] = useState(JSON.stringify(defaultCreate ?? {}, null, 2));
  const [updateJson, setUpdateJson] = useState(JSON.stringify(defaultUpdate ?? {}, null, 2));
  const [out, setOut] = useState('');
  const [toast, setToast] = useState(null);

  async function run(fn){
    setToast(null);
    setOut('');
    try{
      const data = await fn();
      setOut(pretty(data));
      setToast({kind:'ok', title:'OK'});
    }catch(e){
      setToast({kind:'err', title:'Failed', detail: e?.message || String(e)});
      setOut(pretty(e?.payload ?? ''));
    }
  }

  return (
    <div className="card">
      <SectionHeader title={title} desc={desc} />

      <div className="row">
        <div className="field">
          <div className="label">List querystring (opsiyonel)</div>
          <input className="input" value={listQuery} onChange={(e)=>setListQuery(e.target.value)} placeholder="page=0&size=20&name=foo" />
        </div>
        <button className="btn primary" onClick={()=>run(()=>api.get(listQuery ? `${listPath}?${listQuery}` : listPath))}>List</button>
      </div>

      <div className="hr" />

      <div className="row">
        <div className="field" style={{maxWidth:260}}>
          <div className="label">ID</div>
          <input className="input" value={entityId} onChange={(e)=>setEntityId(e.target.value)} />
        </div>
        <button className="btn" onClick={()=>run(()=>api.get(getPath(entityId)))}>Get</button>
        <button className="btn danger" onClick={()=>run(()=>api.del(deletePath(entityId)))}>Delete</button>
      </div>

      <div className="hr" />

      <div className="grid">
        <div>
          <div className="row" style={{justifyContent:'space-between'}}>
            <div className="h1" style={{fontSize:16}}>Create</div>
            <button className="btn primary" onClick={()=>run(()=>api.post(createPath, JSON.parse(createJson || '{}')))}>POST</button>
          </div>
          <JsonBox value={createJson} onChange={setCreateJson} rows={14} />
        </div>

        <div>
          <div className="row" style={{justifyContent:'space-between'}}>
            <div className="h1" style={{fontSize:16}}>Update</div>
            <button className="btn primary" onClick={()=>run(()=>api.put(updatePath(entityId), JSON.parse(updateJson || '{}')))}>PUT</button>
          </div>
          <JsonBox value={updateJson} onChange={setUpdateJson} rows={14} />
        </div>
      </div>

      <div className="hr" />

      <div className="h1" style={{fontSize:16}}>Output</div>
      <pre className="mono" style={{fontSize:12, whiteSpace:'pre-wrap'}}>{out}</pre>
      <Toast kind={toast?.kind} title={toast?.title} detail={toast?.detail} />
    </div>
  );
}

export function Events(){
  return (
    <EntityConsole
      title="Events"
      desc="/events CRUD + list (pagination/filters). DTO alanların projendeki isimleri farklıysa JSON'ı değiştir." 
      listPath="/events"
      getPath={(id)=>`/events/${id}`}
      createPath="/events"
      updatePath={(id)=>`/events/${id}`}
      deletePath={(id)=>`/events/${id}`}
      defaultCreate={{
        type: 'CONCERT',
        name: 'Demo Event',
        summary: 'Short summary',
        description: 'Long description',
        startDate: '2026-02-01T18:00:00Z',
        endDate: '2026-02-01T23:00:00Z'
      }}
      defaultUpdate={{
        name: 'Updated Event Name'
      }}
    />
  );
}

export function Performers(){
  return (
    <EntityConsole
      title="Performers"
      desc="Case dokümanında Performer CRUD var. Endpoint adın /performers değilse Quick API kullan." 
      listPath="/performers"
      getPath={(id)=>`/performers/${id}`}
      createPath="/performers"
      updatePath={(id)=>`/performers/${id}`}
      deletePath={(id)=>`/performers/${id}`}
      defaultCreate={{
        name: 'DJ Alpha',
        role: 'DJ',
        biography: 'Short bio'
      }}
      defaultUpdate={{
        biography: 'Updated bio'
      }}
    />
  );
}

export function Seances(){
  const [eventId, setEventId] = useState('1');
  const [seanceId, setSeanceId] = useState('1');
  const [toast, setToast] = useState(null);
  const [out, setOut] = useState('');
  const [createJson, setCreateJson] = useState(JSON.stringify({ startDateTime:'2026-02-01T18:00:00Z', capacity: 500 }, null, 2));

  async function run(fn){
    setToast(null); setOut('');
    try{ const d = await fn(); setOut(pretty(d)); setToast({kind:'ok', title:'OK'});}catch(e){ setToast({kind:'err', title:'Failed', detail:e?.message||String(e)}); setOut(pretty(e?.payload ?? ''));}
  }

  return (
    <div className="card">
      <SectionHeader
        title="Seances"
        desc="Seance/session işlemleri: create (event'e bağlı), get, list, availability."
      />

      <div className="row">
        <div className="field" style={{maxWidth:240}}>
          <div className="label">eventId</div>
          <input className="input" value={eventId} onChange={e=>setEventId(e.target.value)} />
        </div>
        <button className="btn primary" onClick={()=>run(()=>api.post(`/events/${eventId}/seances`, JSON.parse(createJson||'{}')))}>Create seance</button>
      </div>
      <JsonBox label="Create JSON" value={createJson} onChange={setCreateJson} rows={10} />

      <div className="hr" />

      <div className="row">
        <div className="field" style={{maxWidth:240}}>
          <div className="label">seanceId</div>
          <input className="input" value={seanceId} onChange={e=>setSeanceId(e.target.value)} />
        </div>
        <button className="btn" onClick={()=>run(()=>api.get(`/seances/${seanceId}`))}>Get</button>
        <button className="btn" onClick={()=>run(()=>api.get(`/seances/${seanceId}/availability`))}>Availability</button>
      </div>

      <div className="hr" />

      <div className="row">
        <div className="field" style={{flex:'1 1 520px'}}>
          <div className="label">List query (eventId, dateFrom, dateTo)</div>
          <input className="input" placeholder="eventId=1&dateFrom=2026-02-01T00:00:00Z&dateTo=2026-02-02T00:00:00Z" onChange={(e)=>setOut(e.target.value)} />
        </div>
        <button className="btn primary" onClick={()=>{
          const q = out.trim();
          run(()=>api.get(q ? `/seances?${q}` : '/seances'));
        }}>List</button>
      </div>

      <div className="hr" />
      <div className="h1" style={{fontSize:16}}>Output</div>
      <pre className="mono" style={{fontSize:12, whiteSpace:'pre-wrap'}}>{typeof out === 'string' ? out : pretty(out)}</pre>
      <Toast kind={toast?.kind} title={toast?.title} detail={toast?.detail} />
    </div>
  );
}

export function Holds(){
  const [holdId, setHoldId] = useState('1');
  const [json, setJson] = useState(JSON.stringify({ userId: 123, seanceId: 1, quantity: 2, idempotencyKey: 'demo-key-1' }, null, 2));
  const [out, setOut] = useState('');
  const [toast, setToast] = useState(null);

  async function run(fn){
    setToast(null); setOut('');
    try{ const d = await fn(); setOut(pretty(d)); setToast({kind:'ok', title:'OK'});}catch(e){ setToast({kind:'err', title:'Failed', detail:e?.message||String(e)}); setOut(pretty(e?.payload ?? ''));}
  }

  return (
    <div className="card">
      <SectionHeader
        title="Holds"
        desc="POST /holds, GET /holds/{id}, DELETE /holds/{id}. Idempotency key'ini aynı verince aynı hold dönmeli (case beklentisi)."
      />

      <div className="row">
        <button className="btn primary" onClick={()=>run(()=>api.post('/holds', JSON.parse(json||'{}')))}>Create hold</button>
        <div className="field" style={{maxWidth:260}}>
          <div className="label">holdId</div>
          <input className="input" value={holdId} onChange={e=>setHoldId(e.target.value)} />
        </div>
        <button className="btn" onClick={()=>run(()=>api.get(`/holds/${holdId}`))}>Get</button>
        <button className="btn danger" onClick={()=>run(()=>api.del(`/holds/${holdId}`))}>Release</button>
      </div>

      <JsonBox label="Request JSON" value={json} onChange={setJson} rows={12} />

      <div className="hr" />
      <div className="h1" style={{fontSize:16}}>Output</div>
      <pre className="mono" style={{fontSize:12, whiteSpace:'pre-wrap'}}>{out}</pre>
      <Toast kind={toast?.kind} title={toast?.title} detail={toast?.detail} />
    </div>
  );
}

export function PaymentsPurchases(){
  const [payJson, setPayJson] = useState(JSON.stringify({ amount: 100, currency: 'TRY', cardToken: 'mock' }, null, 2));
  const [purJson, setPurJson] = useState(JSON.stringify({ holdId: 1, paymentRef: 'PAY-REF-001', idempotencyKey: 'purchase-key-1' }, null, 2));
  const [out, setOut] = useState('');
  const [toast, setToast] = useState(null);

  async function run(fn){
    setToast(null); setOut('');
    try{ const d = await fn(); setOut(pretty(d)); setToast({kind:'ok', title:'OK'});}catch(e){ setToast({kind:'err', title:'Failed', detail:e?.message||String(e)}); setOut(pretty(e?.payload ?? ''));}
  }

  return (
    <div className="card">
      <SectionHeader
        title="Payments & Purchases"
        desc="Case'te /payments/authorize (mock) + /purchases (holdId + paymentRef + idempotencyKey) var. Projende alanlar farklıysa JSON'ı uyarla."
      />

      <div className="grid">
        <div>
          <div className="row" style={{justifyContent:'space-between'}}>
            <div className="h1" style={{fontSize:16}}>Authorize Payment</div>
            <button className="btn primary" onClick={()=>run(()=>api.post('/payments/authorize', JSON.parse(payJson||'{}')))}>POST</button>
          </div>
          <JsonBox value={payJson} onChange={setPayJson} rows={12} />
        </div>

        <div>
          <div className="row" style={{justifyContent:'space-between'}}>
            <div className="h1" style={{fontSize:16}}>Purchase</div>
            <button className="btn primary" onClick={()=>run(()=>api.post('/purchases', JSON.parse(purJson||'{}')))}>POST</button>
          </div>
          <JsonBox value={purJson} onChange={setPurJson} rows={12} />
        </div>
      </div>

      <div className="hr" />
      <div className="h1" style={{fontSize:16}}>Output</div>
      <pre className="mono" style={{fontSize:12, whiteSpace:'pre-wrap'}}>{out}</pre>
      <Toast kind={toast?.kind} title={toast?.title} detail={toast?.detail} />
    </div>
  );
}
