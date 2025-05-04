/**
 * PokerDebug.js - Utility functions to help debug the poker game
 */

// Log player positions and states
export const logPlayers = (players, currentPlayerIndex) => {
  if (!players || !players.length) {
    console.log('No players to log');
    return;
  }
  
  console.log('========== PLAYERS DEBUG ==========');
  players.forEach((player, index) => {
    if (!player) {
      console.log(`Player ${index}: null or undefined`);
      return;
    }
    
    console.log(`Player ${index}: ${player.username} ${index === currentPlayerIndex ? '(Current Turn)' : ''}`);
    console.log(`  Chips: ${player.chips}, Folded: ${player.hasFolded}`);
  });
  console.log('===================================');
};

// Check if DOM elements for players exist
export const checkPlayerElements = () => {
  setTimeout(() => {
    const playerElements = document.querySelectorAll('.player');
    console.log(`Found ${playerElements.length} player elements in DOM`);
    
    playerElements.forEach((el, index) => {
      const position = el.getAttribute('data-position');
      const playerName = el.querySelector('.player-name')?.textContent;
      const isVisible = window.getComputedStyle(el).display !== 'none';
      const opacity = window.getComputedStyle(el).opacity;
      
      console.log(`DOM Player ${index}: ${playerName}`);
      console.log(`  Position: ${position}, Visible: ${isVisible}, Opacity: ${opacity}`);
      console.log(`  Bounds: ${el.getBoundingClientRect().top}, ${el.getBoundingClientRect().left}`);
    });
  }, 1000);
};

// Highlight player elements for debugging
export const highlightPlayers = () => {
  setTimeout(() => {
    const playerElements = document.querySelectorAll('.player');
    
    playerElements.forEach((el) => {
      const originalBorder = el.style.border;
      const originalBackground = el.style.background;
      
      // Highlight for a few seconds
      el.style.border = '3px solid red';
      el.style.background = 'rgba(255, 0, 0, 0.3)';
      
      setTimeout(() => {
        el.style.border = originalBorder;
        el.style.background = originalBackground;
      }, 3000);
    });
  }, 1000);
};

export default { logPlayers, checkPlayerElements, highlightPlayers }; 