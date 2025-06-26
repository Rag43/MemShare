import React, { useState, useEffect, useRef } from 'react';
import './CreateGroupModal.css';

interface UserSummary {
  id: number;
  firstname: string;
  lastname: string;
  email: string;
}

interface CreateGroupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onGroupCreated: () => void;
}

const CreateGroupModal: React.FC<CreateGroupModalProps> = ({ isOpen, onClose, onGroupCreated }) => {
  const [groupName, setGroupName] = useState('');
  const [selectedUsers, setSelectedUsers] = useState<UserSummary[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<UserSummary[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);
  const [showSearchResults, setShowSearchResults] = useState(false);
  
  const searchTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (searchQuery.trim().length >= 2) {
      // Debounce search
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
      
      searchTimeoutRef.current = setTimeout(() => {
        searchUsers(searchQuery);
      }, 300);
    } else {
      setSearchResults([]);
      setShowSearchResults(false);
    }

    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    };
  }, [searchQuery]);

  const searchUsers = async (query: string) => {
    setIsSearching(true);
    try {
      const response = await fetch(`/api/v1/groups/search-users?query=${encodeURIComponent(query)}`);
      if (response.ok) {
        const users = await response.json();
        setSearchResults(users);
        setShowSearchResults(true);
      }
    } catch (error) {
      console.error('Error searching users:', error);
    } finally {
      setIsSearching(false);
    }
  };

  const handleUserSelect = (user: UserSummary) => {
    if (!selectedUsers.find(u => u.id === user.id)) {
      setSelectedUsers([...selectedUsers, user]);
    }
    setSearchQuery('');
    setShowSearchResults(false);
    searchInputRef.current?.blur();
  };

  const handleUserRemove = (userId: number) => {
    setSelectedUsers(selectedUsers.filter(user => user.id !== userId));
  };

  const handleCreateGroup = async () => {
    if (selectedUsers.length === 0) {
      setMessage({ text: 'Please select at least one group member', type: 'error' });
      return;
    }

    setIsCreating(true);
    setMessage(null);

    try {
      const token = localStorage.getItem('jwt_token');
      const response = await fetch('/api/v1/groups', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: groupName.trim() || null,
          description: '',
          userIds: selectedUsers.map(user => user.id)
        }),
      });

      if (response.ok) {
        setMessage({ text: 'Group created successfully!', type: 'success' });
        setTimeout(() => {
          onGroupCreated();
          onClose();
          // Reset form
          setGroupName('');
          setSelectedUsers([]);
          setMessage(null);
        }, 1500);
      } else {
        const errorData = await response.json();
        setMessage({ text: errorData.message || 'Failed to create group', type: 'error' });
      }
    } catch (error) {
      setMessage({ text: 'Network error occurred', type: 'error' });
    } finally {
      setIsCreating(false);
    }
  };

  const handleClose = () => {
    if (!isCreating) {
      onClose();
      // Reset form
      setGroupName('');
      setSelectedUsers([]);
      setSearchQuery('');
      setSearchResults([]);
      setMessage(null);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Create a MemGroup</h2>
          <button className="close-button" onClick={handleClose} disabled={isCreating}>
            ×
          </button>
        </div>

        <div className="modal-body">
          <div className="form-group">
            <label htmlFor="groupName">Group Name (Optional)</label>
            <input
              id="groupName"
              type="text"
              value={groupName}
              onChange={(e) => setGroupName(e.target.value)}
              placeholder="Enter group name..."
              disabled={isCreating}
            />
          </div>

          <div className="form-group">
            <label htmlFor="groupMembers">Group Members *</label>
            <div className="search-container">
              <input
                ref={searchInputRef}
                id="groupMembers"
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search users by name or email..."
                disabled={isCreating}
                onFocus={() => {
                  if (searchResults.length > 0) {
                    setShowSearchResults(true);
                  }
                }}
              />
              
              {showSearchResults && (
                <div className="search-results">
                  {isSearching ? (
                    <div className="search-loading">Searching...</div>
                  ) : searchResults.length > 0 ? (
                    searchResults
                      .filter(user => !selectedUsers.find(u => u.id === user.id))
                      .slice(0, 5) // Limit to 5 results for aesthetics
                      .map(user => (
                        <div
                          key={user.id}
                          className="search-result-item"
                          onClick={() => handleUserSelect(user)}
                        >
                          <div className="user-name">
                            {user.firstname} {user.lastname}
                          </div>
                          <div className="user-email">{user.email}</div>
                        </div>
                      ))
                  ) : (
                    <div className="no-results">No users found</div>
                  )}
                </div>
              )}
            </div>

            {selectedUsers.length > 0 && (
              <div className="selected-users">
                {selectedUsers.map(user => (
                  <div key={user.id} className="selected-user">
                    <span>{user.firstname} {user.lastname}</span>
                    <button
                      className="remove-user"
                      onClick={() => handleUserRemove(user.id)}
                      disabled={isCreating}
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {message && (
            <div className={`message ${message.type}`}>
              {message.text}
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button
            className="cancel-button"
            onClick={handleClose}
            disabled={isCreating}
          >
            Cancel
          </button>
          <button
            className="create-button"
            onClick={handleCreateGroup}
            disabled={isCreating || selectedUsers.length === 0}
          >
            {isCreating ? 'Creating...' : 'Create Group'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default CreateGroupModal; 