import React, { useState, useEffect } from 'react';
import './MyMemories.css';
import MemoryModal from './MemoryModal';

interface Media {
  id: number;
  fileName: string;
  s3Key: string;
  s3Bucket: string;
  mediaType: string;
  fileType?: string;
}

interface Memory {
  id: number;
  title: string;
  content: string;
  memoryDate: string;
  location: string;
  displayPic: string | null;
  media: Media[];
  groupName: string; // Added to show which group the memory belongs to
}

interface MyMemoriesProps {
  onBack: () => void;
}

const MyMemories: React.FC<MyMemoriesProps> = ({ onBack }) => {
  const [memories, setMemories] = useState<Memory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedMemory, setSelectedMemory] = useState<Memory | null>(null);

  useEffect(() => {
    fetchAllMemories();
  }, []);

  const fetchAllMemories = async () => {
    try {
      const token = localStorage.getItem('jwt_token');
      if (!token) {
        setError('No authentication token found');
        return;
      }

      const response = await fetch('/api/v1/memories/my-memories', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Received memories data:', data);
        setMemories(data);
      } else {
        setError('Failed to fetch memories');
      }
    } catch (err) {
      setError('Network error occurred');
    } finally {
      setLoading(false);
    }
  };

  const getDisplayImage = (memory: Memory): string | null => {
    console.log('Getting display image for memory:', memory.id, 'displayPic:', memory.displayPic, 'media count:', memory.media?.length);
    
    // First try to use displayPic if set
    if (memory.displayPic) {
      return `https://memshare-media-rag43.s3.amazonaws.com/${memory.displayPic}`;
    }
    
    // If no displayPic, get the last image from media
    const images = memory.media?.filter(m => m.mediaType === 'IMAGE') || [];
    console.log('Filtered images:', images);
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

  const handleMemoryClick = (memory: Memory) => {
    setSelectedMemory(memory);
  };

  const handleCloseMemoryModal = () => {
    setSelectedMemory(null);
  };

  return (
    <div className="my-memories-container">
      <div className="my-memories-header">
        <button className="my-memories-back-button" onClick={onBack}>
          ← Back
        </button>
        <h1 className="my-memories-title">My Memories</h1>
      </div>

      <div className="my-memories-content">
        {loading ? (
          <div className="loading-message">Loading memories...</div>
        ) : error ? (
          <div className="error-message">{error}</div>
        ) : memories.length === 0 ? (
          <div className="no-memories-message">
            You have no memories yet.
          </div>
        ) : (
          <div className="memories-list">
            {memories.map((memory) => {
              const displayImage = getDisplayImage(memory);
              return (
                <div key={memory.id} className="memory-item">
                  <div className="memory-date">
                    {formatDate(memory.memoryDate)}
                  </div>
                  <div className="memory-separator"></div>
                  <button 
                    className="memory-content"
                    onClick={() => handleMemoryClick(memory)}
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
                      <div className="memory-group-name">
                        From: {memory.groupName}
                      </div>
                    </div>
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <MemoryModal
        isOpen={!!selectedMemory}
        onClose={handleCloseMemoryModal}
        memory={selectedMemory}
      />
    </div>
  );
};

export default MyMemories; 