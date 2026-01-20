import React from 'react';

export default function SectionHeader({title, desc, right}){
  return (
    <div className="header">
      <div>
        <div className="h1">{title}</div>
        {desc ? <div className="p">{desc}</div> : null}
      </div>
      <div>{right}</div>
    </div>
  );
}
