import React, { useState, useEffect } from 'react';
import './GroupDetailsModal.css';
import CreateMemoryModal from './CreateMemoryModal';
import MemoryModal from './MemoryModal';
import DeleteConfirmationModal from './DeleteConfirmationModal';
import EditMemoryModal from './EditMemoryModal';
import GroupSettingsModal from './GroupSettingsModal';

interface Media {
  id: number;
  fileName: string;
  s3Key: string;
  s3Bucket: string;
  mediaType: string;
}

interface Memory {
  id: number;
  title: string;
  content: string;
  memoryDate: string;
  location: string;
  displayPic: string | null;
  media: Media[];
  userId: number;
}

interface UserSummary {
  id: number;
  firstname: string;
  lastname: string;
  email: string;
}

interface GroupDetailsModalProps {
  isOpen: boolean;
  onClose: () => void;
  groupId: number;
  groupName: string;
  onGroupDeleted?: () => void;
}

const GroupDetailsModal: React.FC<GroupDetailsModalProps> = ({ 
  isOpen, 
  onClose, 
  groupId, 
  groupName,
  onGroupDeleted
}) => {
  const [memories, setMemories] = useState<Memory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isCreateMemoryOpen, setIsCreateMemoryOpen] = useState(false);
  const [selectedMemory, setSelectedMemory] = useState<Memory | null>(null);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [memoryToDelete, setMemoryToDelete] = useState<Memory | null>(null);
  const [memoryToEdit, setMemoryToEdit] = useState<Memory | null>(null);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const [groupUsers, setGroupUsers] = useState<UserSummary[]>([]);

  useEffect(() => {
    if (isOpen && groupId) {
      fetchGroupMemories();
      getCurrentUserId();
    }
  }, [isOpen, groupId]);

  const fetchGroupMemories = async () => {
    try {
      const token = localStorage.getItem('jwt_token');
      if (!token) {
        setError('No authentication token found');
        return;
      }

      const response = await fetch(`http://localhost:8080/api/v1/memories/group/${groupId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const data = await response.json();
        setMemories(data);
      } else {
        setError('Failed to fetch group memories');
      }
    } catch (err) {
      setError('Network error occurred');
    } finally {
      setLoading(false);
    }
  };

  const getCurrentUserId = async () => {
    try {
      const token = localStorage.getItem('jwt_token');
      if (!token) return;

      const response = await fetch('http://localhost:8080/api/v1/auth/me', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const userData = await response.json();
        setCurrentUserId(userData.id);
      }
    } catch (err) {
      console.error('Error fetching current user:', err);
    }
  };

  const getDisplayImage = (memory: Memory): string | null => {
    // First try to use displayPic if set
    if (memory.displayPic) {
      return `https://memshare-media-rag43.s3.amazonaws.com/${memory.displayPic}`;
    }
    
    // If no displayPic, get the last image from media
    const images = memory.media?.filter(m => m.mediaType === 'IMAGE') || [];
    if (images.length > 0) {
      const lastImage = images[images.length - 1];
      return `https://${lastImage.s3Bucket}.s3.amazonaws.com/${lastImage.s3Key}`;
    }
    
    return null;
  };

  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const handleMemoryClick = (memoryId: number) => {
    const memory = memories.find(m => m.id === memoryId);
    if (memory) setSelectedMemory(memory);
  };

  const handleCreateMemory = () => {
    setIsCreateMemoryOpen(true);
  };

  const handleMemoryCreated = () => {
    // Refresh the memories list
    fetchGroupMemories();
  };

  const handleClose = () => {
    onClose();
    setMemories([]);
    setError(null);
    setLoading(true);
  };

  const handleCloseMemoryModal = () => {
    setSelectedMemory(null);
  };

  const handleDeleteClick = (memory: Memory, e: React.MouseEvent) => {
    e.stopPropagation();
    setMemoryToDelete(memory);
    setIsDeleteModalOpen(true);
  };

  const handleEditClick = (memory: Memory, e: React.MouseEvent) => {
    e.stopPropagation();
    setMemoryToEdit(memory);
    setIsEditModalOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!memoryToDelete) return;

    try {
      const token = localStorage.getItem('jwt_token');
      if (!token) {
        setError('No authentication token found');
        return;
      }

      const response = await fetch(`http://localhost:8080/api/v1/memories/${memoryToDelete.id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        // Refresh the memories list
        fetchGroupMemories();
      } else {
        setError('Failed to delete memory');
      }
    } catch (err) {
      setError('Network error occurred');
    } finally {
      setIsDeleteModalOpen(false);
      setMemoryToDelete(null);
    }
  };

  const handleMemoryUpdated = () => {
    // Refresh the memories list
    fetchGroupMemories();
  };

  const handleSettingsClick = () => {
    // Fetch group details to get users
    fetchGroupDetails();
    setIsSettingsModalOpen(true);
  };

  const fetchGroupDetails = async () => {
    try {
      const token = localStorage.getItem('jwt_token');
      if (!token) return;

      const response = await fetch(`http://localhost:8080/api/v1/groups/${groupId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const groupData = await response.json();
        setGroupUsers(groupData.users);
      }
    } catch (err) {
      console.error('Error fetching group details:', err);
    }
  };

  const handleGroupUpdated = () => {
    // Refresh group details
    fetchGroupDetails();
  };

  const handleGroupDeleted = () => {
    // Close the group details modal and refresh the groups list
    onClose();
    if (onGroupDeleted) {
      onGroupDeleted();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="group-modal-overlay" onClick={handleClose}>
      <div className="group-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="group-modal-header">
          <button className="group-back-button" onClick={handleClose}>
            ← Back
          </button>
          <h2 className="group-title">{groupName}</h2>
          <button className="group-settings-button" onClick={handleSettingsClick}>
            MemGroup Settings
          </button>
        </div>

        <div className="group-modal-body">
          <button className="create-memory-button" onClick={handleCreateMemory}>
            Create a Memory
          </button>
          
          {loading ? (
            <div className="loading-message">Loading memories...</div>
          ) : error ? (
            <div className="error-message">{error}</div>
          ) : memories.length === 0 ? (
            <div className="no-memories-message">
              No memories in this group yet.
            </div>
          ) : (
            <div className="memories-list">
              {memories.map((memory) => {
                const displayImage = getDisplayImage(memory);
                const isOwner = currentUserId === memory.userId;
                
                return (
                  <div key={memory.id} className="memory-item">
                    <div className="memory-date">
                      {formatDate(memory.memoryDate)}
                    </div>
                    <div className="memory-separator"></div>
                    <div className="memory-content-container">
                      <button 
                        className="memory-content"
                        onClick={() => handleMemoryClick(memory.id)}
                      >
                        {displayImage && (
                          <div className="memory-image">
                            <img src={displayImage} alt={memory.title} />
                          </div>
                        )}
                        <div className="memory-info">
                          <div className="memory-title">{memory.title}</div>
                          {memory.location && (
                            <div className="memory-location">{memory.location}</div>
                          )}
                        </div>
                      </button>
                      
                      {isOwner && (
                        <div className="memory-actions">
                          <button
                            className="memory-action-button edit-button"
                            onClick={(e) => handleEditClick(memory, e)}
                            title="Edit Memory"
                          >
                            ✏️
                          </button>
                          <button
                            className="memory-action-button delete-button"
                            onClick={(e) => handleDeleteClick(memory, e)}
                            title="Delete Memory"
                          >
                            🗑️
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      <CreateMemoryModal
        isOpen={isCreateMemoryOpen}
        onClose={() => setIsCreateMemoryOpen(false)}
        onMemoryCreated={handleMemoryCreated}
        groupId={groupId}
      />

      <MemoryModal
        isOpen={!!selectedMemory}
        onClose={handleCloseMemoryModal}
        memory={selectedMemory}
      />

      <DeleteConfirmationModal
        isOpen={isDeleteModalOpen}
        onClose={() => {
          setIsDeleteModalOpen(false);
          setMemoryToDelete(null);
        }}
        onConfirm={handleDeleteConfirm}
        title="Delete Memory"
        message={`Are you sure you want to delete "${memoryToDelete?.title}"? This action cannot be undone.`}
      />

      <EditMemoryModal
        isOpen={isEditModalOpen}
        onClose={() => {
          setIsEditModalOpen(false);
          setMemoryToEdit(null);
        }}
        onMemoryUpdated={handleMemoryUpdated}
        memory={memoryToEdit}
      />

      <GroupSettingsModal
        isOpen={isSettingsModalOpen}
        onClose={() => setIsSettingsModalOpen(false)}
        groupId={groupId}
        groupName={groupName}
        currentUsers={groupUsers}
        onGroupUpdated={handleGroupUpdated}
        onGroupDeleted={handleGroupDeleted}
      />
    </div>
  );
};

export default GroupDetailsModal; 