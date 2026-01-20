import React from 'react';

export default function Toast({kind, title, detail}){
  if(!title && !detail) return null;
  const cls = kind === 'ok' ? 'toast ok' : kind === 'err' ? 'toast err' : 'toast';
  return (
    <div className={cls}>
      {title ? <div style={{fontWeight:700, marginBottom:4}}>{title}</div> : null}
      {detail ? <div className="mono small">{detail}</div> : null}
    </div>
  );
}
