import React, { useState, useRef } from 'react';
import './CreateMemoryModal.css';

interface CreateMemoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onMemoryCreated: () => void;
  groupId: number;
}

interface UploadedFile {
  id: string;
  name: string;
  url: string;
  type: string;
  file: File; // Keep reference to original file for upload
}

interface MediaUploadResponse {
  id: number;
  fileName: string;
  s3Key: string;
  s3Bucket: string;
  mediaType: string;
  getS3Url: () => string;
}

const API_BASE = 'http://localhost:8080';

const CreateMemoryModal: React.FC<CreateMemoryModalProps> = ({ 
  isOpen, 
  onClose, 
  onMemoryCreated,
  groupId 
}) => {
  const [memoryName, setMemoryName] = useState('');
  const [location, setLocation] = useState('');
  const [text, setText] = useState('');
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const [selectedThumbnail, setSelectedThumbnail] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);
  
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileUpload = async (files: FileList) => {
    const newFiles: UploadedFile[] = [];
    
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const fileId = `file-${Date.now()}-${i}`;
      
      // Create a temporary URL for preview
      const tempUrl = URL.createObjectURL(file);
      
      newFiles.push({
        id: fileId,
        name: file.name,
        url: tempUrl,
        type: file.type.startsWith('image/') ? 'image' : 'other',
        file: file // Keep reference to original file
      });
    }
    
    setUploadedFiles(prev => [...prev, ...newFiles]);
  };

  const handleFileRemove = (fileId: string) => {
    setUploadedFiles(prev => {
      const file = prev.find(f => f.id === fileId);
      if (file) {
        URL.revokeObjectURL(file.url);
      }
      return prev.filter(f => f.id !== fileId);
    });
    
    if (selectedThumbnail === fileId) {
      setSelectedThumbnail(null);
    }
  };

  const handleThumbnailSelect = (fileId: string) => {
    console.log('Thumbnail selected:', fileId);
    setSelectedThumbnail(fileId);
  };

  const uploadFileToS3 = async (file: File, memoryId: number): Promise<MediaUploadResponse> => {
    console.log('Uploading file to S3:', file.name, 'for memory:', memoryId);
    const token = localStorage.getItem('jwt_token');
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${API_BASE}/api/v1/media/upload/${memoryId}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
      body: formData,
    });

    console.log('Upload response status:', response.status);
    if (!response.ok) {
      const errorText = await response.text();
      console.error('Upload failed:', errorText);
      throw new Error(`Failed to upload file ${file.name}: ${errorText}`);
    }

    const result = await response.json();
    console.log('Upload successful:', result);
    
    return result;
  };

  const handleCreateMemory = async () => {
    if (!memoryName.trim() || !text.trim()) {
      setMessage({ text: 'Please fill in all required fields', type: 'error' });
      return;
    }

    if (uploadedFiles.length === 0) {
      setMessage({ text: 'Please upload at least one file', type: 'error' });
      return;
    }

    console.log('Creating memory with files:', uploadedFiles.map(f => f.name));
    setIsCreating(true);
    setMessage(null);

    try {
      const token = localStorage.getItem('jwt_token');
      
      // First, create the memory without media
      console.log('Creating memory...');
      const memoryResponse = await fetch(`${API_BASE}/api/v1/memories`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          title: memoryName.trim(),
          content: text.trim(),
          location: location.trim() || null,
          displayPic: null, // Will be set after file upload
          isPublic: false
        }),
      });

      console.log('Memory creation response status:', memoryResponse.status);
      if (!memoryResponse.ok) {
        const errorData = await memoryResponse.json();
        console.error('Memory creation failed:', errorData);
        throw new Error(errorData.message || 'Failed to create memory');
      }

      const memory = await memoryResponse.json();
      console.log('Memory created:', memory);

      // Then upload all files to S3
      console.log('Starting file uploads...');
      const uploadedMedia: MediaUploadResponse[] = [];
      let displayPicS3Key: string | null = null; // Store S3 key locally
      
      for (const uploadedFile of uploadedFiles) {
        try {
          console.log('Uploading file:', uploadedFile.name, 'ID:', uploadedFile.id);
          console.log('Selected thumbnail ID:', selectedThumbnail);
          const mediaResponse = await uploadFileToS3(uploadedFile.file, memory.id);
          uploadedMedia.push(mediaResponse);
          console.log('File uploaded successfully:', uploadedFile.name);
          
          // If this is the selected thumbnail, store its S3 key locally
          if (selectedThumbnail && uploadedFile.id === selectedThumbnail) {
            displayPicS3Key = mediaResponse.s3Key;
            console.log('Stored S3 key for selected thumbnail locally:', displayPicS3Key);
          } else {
            console.log('File is not the selected thumbnail');
          }
        } catch (error) {
          console.error(`Failed to upload ${uploadedFile.name}:`, error);
          // Continue with other files even if one fails
        }
      }

      console.log('All files uploaded:', uploadedMedia);
      console.log('Final displayPicS3Key:', displayPicS3Key);

      // Update the memory with the displayPic if a thumbnail was selected
      if (displayPicS3Key) {
        console.log('Updating memory with displayPic:', displayPicS3Key);
        const updateResponse = await fetch(`${API_BASE}/api/v1/memories/${memory.id}`, {
          method: 'PUT',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            displayPic: displayPicS3Key
          }),
        });

        if (!updateResponse.ok) {
          console.warn('Failed to update memory with displayPic');
        } else {
          console.log('Successfully updated memory with displayPic');
        }
      } else {
        console.log('No displayPic selected or available');
      }

      setMessage({ text: 'Memory created successfully!', type: 'success' });
      setTimeout(() => {
        onMemoryCreated();
        onClose();
        // Reset form
        setMemoryName('');
        setLocation('');
        setText('');
        setUploadedFiles([]);
        setSelectedThumbnail(null);
        setMessage(null);
      }, 1500);
      
    } catch (error) {
      setMessage({ text: error instanceof Error ? error.message : 'Network error occurred', type: 'error' });
    } finally {
      setIsCreating(false);
    }
  };

  const handleClose = () => {
    if (!isCreating) {
      onClose();
      // Reset form
      setMemoryName('');
      setLocation('');
      setText('');
      setUploadedFiles([]);
      setSelectedThumbnail(null);
      setMessage(null);
      
      // Clean up temporary URLs
      uploadedFiles.forEach(file => {
        URL.revokeObjectURL(file.url);
      });
    }
  };

  const imageFiles = uploadedFiles.filter(file => file.type === 'image');

  if (!isOpen) return null;

  return (
    <div className="memory-modal-overlay" onClick={handleClose}>
      <div className="memory-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="memory-modal-header">
          <h2>Create a Memory</h2>
          <button className="close-button" onClick={handleClose} disabled={isCreating}>
            ×
          </button>
        </div>

        <div className="memory-modal-body">
          <div className="form-group">
            <label htmlFor="memoryName">Memory Name *</label>
            <input
              id="memoryName"
              type="text"
              value={memoryName}
              onChange={(e) => setMemoryName(e.target.value)}
              placeholder="Enter memory name..."
              maxLength={50}
              disabled={isCreating}
            />
            <div className="char-count">{memoryName.length}/50</div>
          </div>

          <div className="form-group">
            <label htmlFor="location">Location</label>
            <input
              id="location"
              type="text"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="Enter location..."
              maxLength={50}
              disabled={isCreating}
            />
            <div className="char-count">{location.length}/50</div>
          </div>

          <div className="form-group">
            <label htmlFor="text">Text *</label>
            <textarea
              id="text"
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="Enter memory text..."
              maxLength={500}
              rows={6}
              disabled={isCreating}
            />
            <div className="char-count">{text.length}/500</div>
          </div>

          <div className="form-group">
            <label htmlFor="media">Media *</label>
            <input
              ref={fileInputRef}
              id="media"
              type="file"
              multiple
              accept="image/*,video/*"
              onChange={(e) => e.target.files && handleFileUpload(e.target.files)}
              disabled={isCreating}
              style={{ display: 'none' }}
            />
            <button
              type="button"
              className="file-upload-button"
              onClick={() => fileInputRef.current?.click()}
              disabled={isCreating}
            >
              Choose Files
            </button>
            
            {uploadedFiles.length > 0 && (
              <div className="uploaded-files">
                {uploadedFiles.map((file) => (
                  <div key={file.id} className="uploaded-file">
                    <span>{file.name}</span>
                    <button
                      className="remove-file"
                      onClick={() => handleFileRemove(file.id)}
                      disabled={isCreating}
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="form-group">
            <label>Thumbnail</label>
            {imageFiles.length > 0 ? (
              <div className="thumbnail-selection">
                {imageFiles.map((file) => (
                  <div
                    key={file.id}
                    className={`thumbnail-option ${selectedThumbnail === file.id ? 'selected' : ''}`}
                    onClick={() => handleThumbnailSelect(file.id)}
                  >
                    <img src={file.url} alt={file.name} />
                    <span>{file.name}</span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="no-images-message">
                No picture files selected
              </div>
            )}
          </div>

          {message && (
            <div className={`message ${message.type}`}>
              {message.text}
            </div>
          )}
        </div>

        <div className="memory-modal-footer">
          <button
            className="cancel-button"
            onClick={handleClose}
            disabled={isCreating}
          >
            Cancel
          </button>
          <button
            className="create-button"
            onClick={handleCreateMemory}
            disabled={isCreating || !memoryName.trim() || !text.trim() || uploadedFiles.length === 0}
          >
            {isCreating ? 'Creating...' : 'Create Memory'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default CreateMemoryModal; 