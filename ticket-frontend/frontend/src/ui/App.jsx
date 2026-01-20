import React, { useMemo, useState } from 'react';
import './theme.css';
import Dashboard from './pages/Dashboard.jsx';
import QuickApi from './pages/QuickApi.jsx';
import { Events, Performers, Seances, Holds, PaymentsPurchases } from './pages/Entities.jsx';

const PAGES = [
  { key: 'dashboard', label: 'Dashboard', badge: 'status' },
  { key: 'quick', label: 'Quick API', badge: 'raw' },
  { key: 'events', label: 'Events', badge: '/events' },
  { key: 'performers', label: 'Performers', badge: '/performers' },
  { key: 'seances', label: 'Seances', badge: '/seances' },
  { key: 'holds', label: 'Holds', badge: '/holds' },
  { key: 'pay', label: 'Payments & Purchases', badge: '/payments /purchases' },
];

export default function App(){
  const [page, setPage] = useState('dashboard');
  const content = useMemo(()=>{
    switch(page){
      case 'dashboard': return <Dashboard/>;
      case 'quick': return <QuickApi/>;
      case 'events': return <Events/>;
      case 'performers': return <Performers/>;
      case 'seances': return <Seances/>;
      case 'holds': return <Holds/>;
      case 'pay': return <PaymentsPurchases/>;
      default: return <Dashboard/>;
    }
  },[page]);

  return (
    <div className="container">
      <aside className="sidebar">
        <div className="brand">
          <div className="logo" />
          <div>
            <div className="title">Ticketing Admin UI</div>
            <div className="sub">React + Nginx proxy + Docker Compose</div>
          </div>
        </div>

        <div className="nav">
          {PAGES.map(p => (
            <button key={p.key} className={page === p.key ? 'active' : ''} onClick={()=>setPage(p.key)}>
              <span>{p.label}</span>
              <span className="badge">{p.badge}</span>
            </button>
          ))}
        </div>

        <div style={{marginTop:'auto', paddingTop:14}}>
          <div className="small">
            Tip: Eğer backend endpointlerin farklıysa panik yok. “Quick API” her şeyi çözer.
          </div>
        </div>
      </aside>

      <main className="main">
        {content}
        <div style={{height:20}} />
        <div className="small">© Demo UI — amaç: case backend’ine eşlik eden pratik arayüz.</div>
      </main>
    </div>
  );
}
