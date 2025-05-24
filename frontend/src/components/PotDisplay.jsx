import React, { useState } from 'react';
import { ChipStack } from './PokerChip';
import './PotDisplay.css';

const PotDisplay = ({ game }) => {
  const { pot, pots, players } = game;
  const [showEligibility, setShowEligibility] = useState(false);
  
  // If no pots array or only one pot, show the total pot
  if (!pots || pots.length <= 1) {
    return (
      <div className="pot-area">
        <ChipStack amount={pot} />
        <div className="pot-label">Pot: ${pot}</div>
      </div>
    );
  }
  
  // Get main pot and side pots
  const mainPot = pots[0];
  const sidePots = pots.slice(1);
  
  // Map player IDs to usernames for display
  const getEligiblePlayerNames = (eligiblePlayerIds) => {
    if (!players || !Array.isArray(players)) return [];
    
    return players
      .filter(player => eligiblePlayerIds.includes(player.id))
      .map(player => player.username);
  };
  
  const toggleEligibility = () => {
    setShowEligibility(!showEligibility);
  };
  
  return (
    <div className="pots-container">
      <div className="main-pot">
        <ChipStack amount={mainPot.amount} />
        <div className="pot-label" onClick={toggleEligibility}>
          Main: ${mainPot.amount}
        </div>
        
        {showEligibility && mainPot.eligiblePlayerIds && mainPot.eligiblePlayerIds.length > 0 && (
          <div className="eligibility-tooltip">
            <div className="tooltip-title">Eligible Players:</div>
            <div className="eligible-players">
              {getEligiblePlayerNames(mainPot.eligiblePlayerIds).join(', ')}
            </div>
          </div>
        )}
      </div>
      
      {sidePots.length > 0 && (
        <div className="side-pots">
          {sidePots.map((sidePot, index) => (
            <div key={`side-pot-${index}`} className="side-pot">
              <ChipStack amount={sidePot.amount} />
              <div className="pot-label side-pot-label" onClick={toggleEligibility}>
                Side {index + 1}: ${sidePot.amount}
              </div>
              
              {showEligibility && sidePot.eligiblePlayerIds && sidePot.eligiblePlayerIds.length > 0 && (
                <div className="eligibility-tooltip">
                  <div className="tooltip-title">Eligible Players:</div>
                  <div className="eligible-players">
                    {getEligiblePlayerNames(sidePot.eligiblePlayerIds).join(', ')}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
      
      <div className="total-pot">
        <div className="pot-label total-pot-label">Total: ${pot}</div>
      </div>
      
      <div className="pot-info-hint">
        <span onClick={toggleEligibility}>
          {showEligibility ? 'Hide eligibility' : 'Show eligibility'}
        </span>
      </div>
    </div>
  );
};

export default PotDisplay; 