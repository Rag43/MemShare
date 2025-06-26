import React, { useState, useEffect } from 'react';
import './MemGroups.css';
import CreateGroupModal from './CreateGroupModal';
import GroupDetailsModal from './GroupDetailsModal';

interface UserSummary {
  id: number;
  firstname: string;
  lastname: string;
  email: string;
}

interface GroupResponse {
  id: number;
  name: string;
  description: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  users: UserSummary[];
}

interface MemGroupsProps {
  onNavigate: (screen: string) => void;
  onBack: () => void;
}

const MemGroups: React.FC<MemGroupsProps> = ({ onNavigate, onBack }) => {
  const [groups, setGroups] = useState<GroupResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState<GroupResponse | null>(null);
  const [isGroupDetailsOpen, setIsGroupDetailsOpen] = useState(false);

  useEffect(() => {
    fetchMyGroups();
  }, []);

  const fetchMyGroups = async () => {
    try {
      const token = localStorage.getItem('jwt_token');
      if (!token) {
        setError('No authentication token found');
        return;
      }

      const response = await fetch('/api/v1/groups/my-groups', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const data = await response.json();
        setGroups(data);
      } else {
        setError('Failed to fetch groups');
      }
    } catch (err) {
      setError('Network error occurred');
    } finally {
      setLoading(false);
    }
  };

  const getGroupDisplayName = (group: GroupResponse): string => {
    if (group.name && group.name.trim()) {
      return group.name;
    }
    
    // If no name, create display name from user names
    const userNames = group.users.map(user => `${user.firstname} ${user.lastname}`).join(', ');
    
    // Limit to 30 characters and add "..." if truncated
    if (userNames.length > 30) {
      return userNames.substring(0, 27) + '...';
    }
    
    return userNames;
  };

  const handleCreateGroup = () => {
    setIsCreateModalOpen(true);
  };

  const handleGroupCreated = () => {
    // Refresh the groups list
    fetchMyGroups();
  };

  const handleGroupClick = (group: GroupResponse) => {
    setSelectedGroup(group);
    setIsGroupDetailsOpen(true);
  };

  const handleGroupDetailsClose = () => {
    setIsGroupDetailsOpen(false);
    setSelectedGroup(null);
  };

  if (loading) {
    return (
      <div className="memshare-bg">
        <div className="memgroups-container">
          <div className="loading-message">Loading your MemGroups...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="memshare-bg">
      <div className="memgroups-container">
        <div className="memgroups-header">
          <button className="back-button" onClick={onBack}>
            Back to Menu
          </button>
        </div>
        
        <div className="memgroups-title">
          Your MemGroups
        </div>
        
        <button className="create-group-button" onClick={handleCreateGroup}>
          Create a MemGroup
        </button>
        
        {error && (
          <div className="error-message">
            {error}
          </div>
        )}
        
        <div className="groups-list">
          {groups.length === 0 ? (
            <div className="no-groups-message">
              You have no MemGroups!
            </div>
          ) : (
            groups.map((group) => (
              <button
                key={group.id}
                className="group-button"
                onClick={() => handleGroupClick(group)}
              >
                {getGroupDisplayName(group)}
              </button>
            ))
          )}
        </div>
      </div>

      <CreateGroupModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onGroupCreated={handleGroupCreated}
      />

      {selectedGroup && (
        <GroupDetailsModal
          isOpen={isGroupDetailsOpen}
          onClose={handleGroupDetailsClose}
          groupId={selectedGroup.id}
          groupName={getGroupDisplayName(selectedGroup)}
          onGroupDeleted={handleGroupCreated}
        />
      )}
    </div>
  );
};

export default MemGroups; 