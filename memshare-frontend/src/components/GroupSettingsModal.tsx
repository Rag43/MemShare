import React, { useState, useEffect } from 'react';
import './GroupSettingsModal.css';
import DeleteConfirmationModal from './DeleteConfirmationModal';

interface UserSummary {
  id: number;
  firstname: string;
  lastname: string;
  email: string;
}

interface GroupSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  groupId: number;
  groupName: string;
  currentUsers: UserSummary[];
  onGroupUpdated: () => void;
  onGroupDeleted: () => void;
}

const GroupSettingsModal: React.FC<GroupSettingsModalProps> = ({
  isOpen,
  onClose,
  groupId,
  groupName,
  currentUsers,
  onGroupUpdated,
  onGroupDeleted
}) => {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isAddUserModalOpen, setIsAddUserModalOpen] = useState(false);
  const [isRemoveUserModalOpen, setIsRemoveUserModalOpen] = useState(false);
  const [isDeleteGroupModalOpen, setIsDeleteGroupModalOpen] = useState(false);
  const [userToRemove, setUserToRemove] = useState<UserSummary | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<UserSummary[]>([]);
  const [searching, setSearching] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setUsers(currentUsers);
      setError(null);
    }
  }, [isOpen, currentUsers]);

  const handleRemoveUser = (user: UserSummary) => {
    setUserToRemove(user);
    setIsRemoveUserModalOpen(true);
  };

  const handleRemoveUserConfirm = async () => {
    if (!userToRemove) return;

    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('jwt_token');
      if (!token) {
        setError('No authentication token found');
        return;
      }

      const response = await fetch(`http://localhost:8080/api/v1/groups/${groupId}/remove-user`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          userId: userToRemove.id
        }),
      });

      if (response.ok) {
        setUsers(prev => prev.filter(u => u.id !== userToRemove.id));
        onGroupUpdated();
      } else {
        const errorData = await response.text();
        setError(`Failed to remove user: ${errorData}`);
      }
    } catch (err) {
      setError('Network error occurred');
    } finally {
      setLoading(false);
      setIsRemoveUserModalOpen(false);
      setUserToRemove(null);
    }
  };

  const handleAddUser = () => {
    setIsAddUserModalOpen(true);
  };

  const handleSearchUsers = async (query: string) => {
    setSearchQuery(query);
    
    if (query.trim().length < 2) {
      setSearchResults([]);
      return;
    }

    setSearching(true);
    try {
      const token = localStorage.getItem('jwt_token');
      if (!token) return;

      const response = await fetch(`http://localhost:8080/api/v1/groups/search-users?query=${encodeURIComponent(query)}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const searchResults = await response.json();
        // Filter out users who are already in the group
        const filteredResults = searchResults.filter((user: UserSummary) => 
          !users.some(existingUser => existingUser.id === user.id)
        );
        setSearchResults(filteredResults);
      }
    } catch (err) {
      console.error('Error searching users:', err);
    } finally {
      setSearching(false);
    }
  };

  const handleAddUserToGroup = async (user: UserSummary) => {
    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('jwt_token');
      if (!token) {
        setError('No authentication token found');
        return;
      }

      const response = await fetch(`http://localhost:8080/api/v1/groups/${groupId}/add-users`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          userIds: [user.id]
        }),
      });

      if (response.ok) {
        setUsers(prev => [...prev, user]);
        setSearchResults(prev => prev.filter(u => u.id !== user.id));
        setSearchQuery('');
        onGroupUpdated();
      } else {
        const errorData = await response.text();
        setError(`Failed to add user: ${errorData}`);
      }
    } catch (err) {
      setError('Network error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteGroup = () => {
    setIsDeleteGroupModalOpen(true);
  };

  const handleDeleteGroupConfirm = async () => {
    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('jwt_token');
      if (!token) {
        setError('No authentication token found');
        return;
      }

      const response = await fetch(`http://localhost:8080/api/v1/groups/${groupId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        onGroupDeleted();
        onClose();
      } else {
        const errorData = await response.text();
        setError(`Failed to delete group: ${errorData}`);
      }
    } catch (err) {
      setError('Network error occurred');
    } finally {
      setLoading(false);
      setIsDeleteGroupModalOpen(false);
    }
  };

  const handleClose = () => {
    if (!loading) {
      setError(null);
      setSearchQuery('');
      setSearchResults([]);
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="group-settings-modal-overlay" onClick={handleClose}>
      <div className="group-settings-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="group-settings-modal-header">
          <h2 className="group-settings-modal-title">MemGroup Settings</h2>
          <button className="group-settings-modal-close" onClick={handleClose} disabled={loading}>
            ×
          </button>
        </div>

        <div className="group-settings-modal-body">
          {/* Current Users Section */}
          <div className="settings-section">
            <h3 className="section-title">Group Members</h3>
            <div className="users-list">
              {users.map((user) => (
                <div key={user.id} className="user-item">
                  <div className="user-info">
                    <div className="user-name">{user.firstname} {user.lastname}</div>
                    <div className="user-email">{user.email}</div>
                  </div>
                  <button
                    className="remove-user-button"
                    onClick={() => handleRemoveUser(user)}
                    disabled={loading}
                    title="Remove user from group"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
            
            <button
              className="add-user-button"
              onClick={handleAddUser}
              disabled={loading}
            >
              + Add User
            </button>
          </div>

          {/* Add User Modal */}
          {isAddUserModalOpen && (
            <div className="add-user-modal-overlay" onClick={() => setIsAddUserModalOpen(false)}>
              <div className="add-user-modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="add-user-modal-header">
                  <h3>Add User to Group</h3>
                  <button 
                    className="add-user-modal-close" 
                    onClick={() => setIsAddUserModalOpen(false)}
                    disabled={loading}
                  >
                    ×
                  </button>
                </div>
                
                <div className="add-user-modal-body">
                  <div className="search-section">
                    <input
                      type="text"
                      placeholder="Search users by name or email..."
                      value={searchQuery}
                      onChange={(e) => {
                        setSearchQuery(e.target.value);
                        handleSearchUsers(e.target.value);
                      }}
                      className="search-input"
                      disabled={loading}
                    />
                  </div>
                  
                  {searching && (
                    <div className="searching-message">Searching...</div>
                  )}
                  
                  {searchResults.length > 0 && (
                    <div className="search-results">
                      {searchResults.map((user) => (
                        <div key={user.id} className="search-result-item">
                          <div className="search-result-info">
                            <div className="search-result-name">{user.firstname} {user.lastname}</div>
                            <div className="search-result-email">{user.email}</div>
                          </div>
                          <button
                            className="add-search-result-button"
                            onClick={() => handleAddUserToGroup(user)}
                            disabled={loading}
                          >
                            Add
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                  
                  {searchQuery && !searching && searchResults.length === 0 && (
                    <div className="no-results-message">No users found</div>
                  )}
                </div>
              </div>
            </div>
          )}

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}
        </div>

        <div className="group-settings-modal-footer">
          <button
            className="delete-group-button"
            onClick={handleDeleteGroup}
            disabled={loading}
          >
            Delete Group
          </button>
        </div>
      </div>

      {/* Remove User Confirmation Modal */}
      <DeleteConfirmationModal
        isOpen={isRemoveUserModalOpen}
        onClose={() => {
          setIsRemoveUserModalOpen(false);
          setUserToRemove(null);
        }}
        onConfirm={handleRemoveUserConfirm}
        title="Remove User"
        message={`Are you sure you want to remove ${userToRemove?.firstname} ${userToRemove?.lastname} from this group?`}
      />

      {/* Delete Group Confirmation Modal */}
      <DeleteConfirmationModal
        isOpen={isDeleteGroupModalOpen}
        onClose={() => setIsDeleteGroupModalOpen(false)}
        onConfirm={handleDeleteGroupConfirm}
        title="Delete MemGroup"
        message={`Are you sure you want to delete "${groupName}"? This action cannot be undone and will remove all memories in this group.`}
      />
    </div>
  );
};

export default GroupSettingsModal; 