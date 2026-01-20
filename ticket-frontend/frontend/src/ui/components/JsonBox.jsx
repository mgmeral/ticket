import React from 'react';

export default function JsonBox({label, value, onChange, rows=10, placeholder}){
  return (
    <div className="field">
      {label ? <div className="label">{label}</div> : null}
      <textarea
        className="textarea"
        rows={rows}
        value={value}
        placeholder={placeholder}
        onChange={(e)=>onChange?.(e.target.value)}
      />
    </div>
  );
}
