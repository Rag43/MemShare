import React, { useState } from 'react';
import './MemoryModal.css';

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
}

interface MemoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  memory: Memory | null;
}

const MemoryModal: React.FC<MemoryModalProps> = ({ isOpen, onClose, memory }) => {
  const [currentIndex, setCurrentIndex] = useState(0);

  if (!isOpen || !memory) return null;

  // Only show images and videos in the carousel
  const mediaList = (memory.media || []).filter(m => m.mediaType === 'IMAGE' || m.mediaType === 'VIDEO');
  const currentMedia = mediaList[currentIndex];

  const handlePrev = () => {
    if (currentIndex > 0) setCurrentIndex(currentIndex - 1);
  };
  const handleNext = () => {
    if (currentIndex < mediaList.length - 1) setCurrentIndex(currentIndex + 1);
  };

  // S3 URL helper
  const getMediaUrl = (media: Media) => `https://${media.s3Bucket}.s3.amazonaws.com/${media.s3Key}`;

  return (
    <div className="memory-modal-overlay" onClick={onClose}>
      <div className="memory-modal-content" onClick={e => e.stopPropagation()}>
        <div className="memory-modal-header">
          <button className="memory-back-button" onClick={onClose}>
            ← Back
          </button>
          <h2 className="memory-title">{memory.title}</h2>
        </div>
        <div className="memory-carousel-panel">
          <button
            className="carousel-arrow left"
            onClick={handlePrev}
            disabled={currentIndex === 0}
          >
            &#8592;
          </button>
          <div className="carousel-media">
            {currentMedia ? (
              currentMedia.mediaType === 'IMAGE' ? (
                <img src={getMediaUrl(currentMedia)} alt={currentMedia.fileName} />
              ) : (
                <video controls src={getMediaUrl(currentMedia)} />
              )
            ) : (
              <div className="no-media">No media available</div>
            )}
          </div>
          <button
            className="carousel-arrow right"
            onClick={handleNext}
            disabled={currentIndex === mediaList.length - 1 || mediaList.length === 0}
          >
            &#8594;
          </button>
        </div>
        <div className="memory-modal-body">
          <div className="memory-content-text">
            {memory.content}
          </div>
        </div>
      </div>
    </div>
  );
};

export default MemoryModal; 