import React from 'react';
import './MainMenu.css';

interface MainMenuProps {
  username: string;
  onNavigate: (screen: string) => void;
  onLogout: () => void;
}

const MainMenu: React.FC<MainMenuProps> = ({ username, onNavigate, onLogout }) => {
  const handleLogout = () => {
    localStorage.removeItem('jwt_token');
    onLogout();
  };

  return (
    <div className="memshare-bg">
      <div className="main-menu-container">
        <div className="main-menu-title">
          <span>MemShare</span>
        </div>
        
        <div className="welcome-message">
          Welcome, <span className="username">{username}</span>
        </div>
        
        <div className="menu-buttons">
          <button 
            className="menu-button"
            onClick={() => onNavigate('memgroups')}
          >
            MemGroups
          </button>
          
          <button 
            className="menu-button"
            onClick={() => onNavigate('my-memories')}
          >
            My Memories
          </button>
          
          <button 
            className="menu-button logout-button"
            onClick={handleLogout}
          >
            Log Out
          </button>
        </div>
      </div>
    </div>
  );
};

export default MainMenu; 