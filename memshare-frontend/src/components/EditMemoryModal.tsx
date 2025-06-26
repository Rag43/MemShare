import React, { useState, useEffect } from 'react';
import './EditMemoryModal.css';

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

interface EditMemoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onMemoryUpdated: () => void;
  memory: Memory | null;
}

const EditMemoryModal: React.FC<EditMemoryModalProps> = ({
  isOpen,
  onClose,
  onMemoryUpdated,
  memory
}) => {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [memoryDate, setMemoryDate] = useState('');
  const [location, setLocation] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mediaFiles, setMediaFiles] = useState<File[]>([]);
  const [existingMedia, setExistingMedia] = useState<Media[]>([]);
  const [uploadingMedia, setUploadingMedia] = useState(false);
  const [mediaToDelete, setMediaToDelete] = useState<number[]>([]);
  const [selectedDisplayPic, setSelectedDisplayPic] = useState<string | null>(null);

  useEffect(() => {
    if (memory) {
      setTitle(memory.title || '');
      setContent(memory.content || '');
      setMemoryDate(memory.memoryDate ? memory.memoryDate.split('T')[0] : '');
      setLocation(memory.location || '');
      setExistingMedia(memory.media || []);
      setMediaFiles([]);
      setMediaToDelete([]);
      setSelectedDisplayPic(memory.displayPic);
    }
  }, [memory]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!memory) return;

    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('jwt_token');
      if (!token) {
        setError('No authentication token found');
        return;
      }

      // First, update the memory details
      const updateData = {
        title: title.trim(),
        content: content.trim(),
        memoryDate: memoryDate ? new Date(memoryDate).toISOString() : null,
        location: location.trim() || null,
        displayPic: selectedDisplayPic
      };

      console.log('Updating memory with displayPic:', selectedDisplayPic);

      const response = await fetch(`http://localhost:8080/api/v1/memories/${memory.id}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(updateData),
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`Failed to update memory: ${errorData}`);
      }

      // Then handle media operations
      await deleteMediaFiles();
      await uploadMediaFiles(memory.id);

      onMemoryUpdated();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Network error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setError(null);
    setLoading(false);
    setMediaFiles([]);
    setMediaToDelete([]);
    onClose();
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    setMediaFiles(prev => [...prev, ...files]);
  };

  const handleRemoveNewFile = (index: number) => {
    setMediaFiles(prev => prev.filter((_, i) => i !== index));
  };

  const handleRemoveExistingMedia = (mediaId: number) => {
    setMediaToDelete(prev => [...prev, mediaId]);
    setExistingMedia(prev => prev.filter(m => m.id !== mediaId));
  };

  const handleDisplayPicSelect = (s3Key: string) => {
    setSelectedDisplayPic(s3Key);
  };

  const uploadMediaFiles = async (memoryId: number): Promise<void> => {
    if (mediaFiles.length === 0) return;

    setUploadingMedia(true);
    const token = localStorage.getItem('jwt_token');
    if (!token) throw new Error('No authentication token found');

    for (const file of mediaFiles) {
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch(`http://localhost:8080/api/v1/media/upload/${memoryId}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`Failed to upload ${file.name}`);
      }
    }
    setUploadingMedia(false);
  };

  const deleteMediaFiles = async (): Promise<void> => {
    if (mediaToDelete.length === 0) return;

    const token = localStorage.getItem('jwt_token');
    if (!token) throw new Error('No authentication token found');

    for (const mediaId of mediaToDelete) {
      const response = await fetch(`http://localhost:8080/api/v1/media/${mediaId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to delete media ${mediaId}`);
      }
    }
  };

  if (!isOpen || !memory) return null;

  return (
    <div className="edit-memory-modal-overlay" onClick={handleClose}>
      <div className="edit-memory-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="edit-memory-modal-header">
          <h2 className="edit-memory-modal-title">Update Memory</h2>
          <button className="edit-memory-modal-close" onClick={handleClose}>
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="edit-memory-form">
          <div className="form-group">
            <label htmlFor="title">Title *</label>
            <input
              type="text"
              id="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Enter memory title"
              required
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label htmlFor="content">Content *</label>
            <textarea
              id="content"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Share your memory..."
              required
              rows={4}
              className="form-textarea"
            />
          </div>

          <div className="form-group">
            <label htmlFor="memoryDate">Memory Date</label>
            <input
              type="date"
              id="memoryDate"
              value={memoryDate}
              onChange={(e) => setMemoryDate(e.target.value)}
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label htmlFor="location">Location</label>
            <input
              type="text"
              id="location"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="Where did this happen?"
              className="form-input"
            />
          </div>

          {/* Existing Media Section */}
          {existingMedia.length > 0 && (
            <div className="form-group">
              <label>Current Media</label>
              <div className="existing-media-grid">
                {existingMedia.map((media) => (
                  <div key={media.id} className="existing-media-item">
                    {media.mediaType === 'IMAGE' ? (
                      <img 
                        src={`https://${media.s3Bucket}.s3.amazonaws.com/${media.s3Key}`} 
                        alt={media.fileName}
                        className="existing-media-preview"
                      />
                    ) : (
                      <div className="existing-media-preview video-placeholder">
                        📹 {media.fileName}
                      </div>
                    )}
                    <button
                      type="button"
                      className="remove-media-button"
                      onClick={() => handleRemoveExistingMedia(media.id)}
                      title="Remove media"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* New Media Upload Section */}
          <div className="form-group">
            <label htmlFor="media-files">Add Media Files</label>
            <input
              type="file"
              id="media-files"
              multiple
              accept="image/*,video/*"
              onChange={handleFileSelect}
              className="form-file-input"
            />
            {mediaFiles.length > 0 && (
              <div className="new-media-grid">
                {mediaFiles.map((file, index) => (
                  <div key={index} className="new-media-item">
                    <div className="new-media-preview">
                      {file.type.startsWith('image/') ? (
                        <img 
                          src={URL.createObjectURL(file)} 
                          alt={file.name}
                          className="new-media-image"
                        />
                      ) : (
                        <div className="new-media-video">
                          📹 {file.name}
                        </div>
                      )}
                    </div>
                    <button
                      type="button"
                      className="remove-new-media-button"
                      onClick={() => handleRemoveNewFile(index)}
                      title="Remove file"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Display Picture Selection */}
          {existingMedia.length > 0 && (
            <div className="form-group">
              <label>Display Picture (Thumbnail)</label>
              <div className="display-pic-grid">
                {existingMedia.filter(m => m.mediaType === 'IMAGE').map((media) => (
                  <div 
                    key={media.id} 
                    className={`display-pic-option ${selectedDisplayPic === media.s3Key ? 'selected' : ''}`}
                    onClick={() => handleDisplayPicSelect(media.s3Key)}
                  >
                    <img 
                      src={`https://${media.s3Bucket}.s3.amazonaws.com/${media.s3Key}`} 
                      alt={media.fileName}
                      className="display-pic-preview"
                    />
                    <div className="display-pic-label">
                      {selectedDisplayPic === media.s3Key ? '✓ Selected' : 'Click to select'}
                    </div>
                  </div>
                ))}
              </div>
              {existingMedia.filter(m => m.mediaType === 'IMAGE').length === 0 && (
                <div className="no-images-message">
                  No image files available for display picture
                </div>
              )}
            </div>
          )}

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          <div className="form-actions">
            <button
              type="button"
              onClick={handleClose}
              className="cancel-button"
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="update-button"
              disabled={loading || uploadingMedia}
            >
              {loading ? 'Updating...' : uploadingMedia ? 'Uploading Media...' : 'Update Memory'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EditMemoryModal; 