import React from 'react';
import './PokerChip.css';

const PokerChip = ({ amount, index, chipRef }) => {
  return (
    <div 
      className="poker-chip" 
      ref={chipRef}
      style={{
        transform: `translateY(${index * -3}px)`, // Stack closer together
        zIndex: index
      }}
    />
  );
};

const ChipStack = ({ amount }) => {
  // Move useRef before the conditional return
  const chipRefs = React.useRef([]);
  
  if (!amount || amount <= 0) return null;
  
  // Calculate how many chips to show based on amount
  const chipCount = Math.min(Math.ceil(amount / 20), 4); // Fewer chips
  
  const chips = [];
  
  for (let i = 0; i < chipCount; i++) {
    chips.push(
      <PokerChip 
        key={i} 
        index={i}
        chipRef={el => {
          if (chipRefs.current.length < chipCount) {
            chipRefs.current.push(el);
          }
        }}
      />
    );
  }
  
  return (
    <div className="chip-stack">
      {chips}
    </div>
  );
};

export { PokerChip, ChipStack }; 