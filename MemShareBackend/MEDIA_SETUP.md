# Media and Memory Setup with AWS S3

This document explains the complete setup for handling media files (images, videos, audio) in the MemShare application using AWS S3 for storage.

## Architecture Overview

### Database Schema

#### Memory Entity
- **id**: Primary key
- **content**: Text content of the memory (TEXT)
- **title**: Memory title
- **memory_date**: When the memory occurred
- **location**: Optional location
- **is_public**: Whether the memory is public
- **user_id**: Foreign key to users table
- **created_at/updated_at**: Timestamps

#### Media Entity
- **id**: Primary key
- **file_name**: Generated unique filename
- **original_file_name**: Original uploaded filename
- **file_type**: MIME type
- **s3_key**: S3 object key
- **s3_bucket**: S3 bucket name
- **file_size**: File size in bytes
- **media_type**: Enum (IMAGE, VIDEO, AUDIO, DOCUMENT)
- **width/height**: For images/videos
- **duration**: For videos/audio
- **thumbnail_s3_key**: For video thumbnails
- **memory_id**: Foreign key to memories table
- **created_at/updated_at**: Timestamps

### S3 Storage Structure
```
s3://your-bucket/
├── users/
│   ├── {user_id}/
│   │   ├── memories/
│   │   │   ├── {memory_id}/
│   │   │   │   ├── {uuid}.jpg
│   │   │   │   ├── {uuid}.mp4
│   │   │   │   └── thumb_{uuid}.jpg
```

## Setup Instructions

### 1. AWS S3 Configuration

#### Create S3 Bucket
1. Go to AWS S3 Console
2. Create a new bucket with appropriate name
3. Configure bucket settings:
   - Enable versioning (optional)
   - Configure lifecycle policies (optional)
   - Set up CORS if needed

#### Configure IAM User
Create an IAM user with S3 permissions:
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:PutObject",
                "s3:DeleteObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::your-bucket-name",
                "arn:aws:s3:::your-bucket-name/*"
            ]
        }
    ]
}
```

#### Environment Variables
Set these environment variables:
```bash
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
```

### 2. Application Configuration

Update `application.properties`:
```properties
# AWS S3 Configuration
aws.s3.bucket-name=your-s3-bucket-name
aws.s3.region=us-east-1
aws.access-key-id=${AWS_ACCESS_KEY_ID:}
aws.secret-access-key=${AWS_SECRET_ACCESS_KEY:}

# File Upload Configuration
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
spring.servlet.multipart.enabled=true

# Media Configuration
media.allowed-types=image/jpeg,image/png,image/gif,image/webp,video/mp4,video/avi,video/mov,video/wmv,audio/mp3,audio/wav,audio/m4a
media.max-file-size=100MB
media.presigned-url-expiration=60
```

### 3. Database Migration

Run the migration to create tables:
```bash
./mvnw flyway:migrate
```

## API Endpoints

### Media Upload
```http
POST /api/v1/media/upload/{memoryId}
Content-Type: multipart/form-data

file: [binary file]
```

### Multiple Files Upload
```http
POST /api/v1/media/upload-multiple/{memoryId}
Content-Type: multipart/form-data

files: [binary files]
```

### Video with Thumbnail Upload
```http
POST /api/v1/media/upload-video/{memoryId}
Content-Type: multipart/form-data

video: [video file]
thumbnail: [thumbnail image] (optional)
```

### Get Media for Memory
```http
GET /api/v1/media/memory/{memoryId}
Authorization: Bearer {jwt-token}
```

### Download File
```http
GET /api/v1/media/download/{mediaId}
Authorization: Bearer {jwt-token}
```

### Generate Presigned URL
```http
GET /api/v1/media/presigned-url/{mediaId}?expirationInMinutes=60
Authorization: Bearer {jwt-token}
```

### Delete Media
```http
DELETE /api/v1/media/{mediaId}
Authorization: Bearer {jwt-token}
```

### Get Media Statistics
```http
GET /api/v1/media/statistics/{memoryId}
Authorization: Bearer {jwt-token}
```

## Usage Examples

### Frontend Integration

#### Upload Single File
```javascript
const uploadFile = async (memoryId, file) => {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await fetch(`/api/v1/media/upload/${memoryId}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });
  
  return response.json();
};
```

#### Upload Multiple Files
```javascript
const uploadMultipleFiles = async (memoryId, files) => {
  const formData = new FormData();
  files.forEach(file => {
    formData.append('files', file);
  });
  
  const response = await fetch(`/api/v1/media/upload-multiple/${memoryId}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });
  
  return response.json();
};
```

#### Display Media
```javascript
const displayMedia = async (memoryId) => {
  const response = await fetch(`/api/v1/media/memory/${memoryId}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const mediaList = await response.json();
  
  mediaList.forEach(media => {
    if (media.mediaType === 'IMAGE') {
      // Display image
      const img = document.createElement('img');
      img.src = media.s3Url; // Direct S3 URL
      // or use presigned URL for security
    } else if (media.mediaType === 'VIDEO') {
      // Display video
      const video = document.createElement('video');
      video.src = media.s3Url;
      video.poster = media.thumbnailUrl; // Use thumbnail
    }
  });
};
```

## Security Considerations

### 1. File Type Validation
- Only allow specific MIME types
- Validate file extensions
- Check file signatures

### 2. File Size Limits
- Set appropriate max file sizes
- Monitor storage usage
- Implement cleanup policies

### 3. Access Control
- Use presigned URLs for secure access
- Implement proper authentication
- Validate user ownership

### 4. S3 Security
- Use IAM roles with minimal permissions
- Enable S3 bucket encryption
- Configure bucket policies

## Performance Optimization

### 1. CDN Integration
Consider using CloudFront for better performance:
```javascript
// Use CloudFront URL instead of direct S3 URL
const cdnUrl = `https://your-cloudfront-domain.com/${media.s3Key}`;
```

### 2. Image Optimization
Implement image resizing and compression:
```java
// In S3Service
public Media uploadImageWithResize(MultipartFile file, Long memoryId, Long userId) {
    // Resize image before upload
    BufferedImage resizedImage = resizeImage(file, 1920, 1080);
    // Upload resized image
}
```

### 3. Video Processing
Consider using AWS MediaConvert for video processing:
- Generate thumbnails
- Create multiple resolutions
- Compress videos

## Monitoring and Maintenance

### 1. Storage Monitoring
- Monitor S3 bucket size
- Track file upload/download metrics
- Set up CloudWatch alarms

### 2. Cleanup Jobs
```java
@Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
public void cleanupOrphanedMedia() {
    mediaService.cleanupOrphanedMedia();
}
```

### 3. Backup Strategy
- Enable S3 versioning
- Set up cross-region replication
- Regular database backups

## Troubleshooting

### Common Issues

1. **File Upload Fails**
   - Check file size limits
   - Verify allowed file types
   - Check S3 permissions

2. **Presigned URL Expires**
   - Increase expiration time
   - Implement refresh mechanism

3. **Memory Issues**
   - Monitor heap usage
   - Implement streaming for large files
   - Use async processing

### Debug Logging
Enable debug logging in `application.properties`:
```properties
logging.level.com.rag.JwtLearn.media=DEBUG
logging.level.software.amazon.awssdk=DEBUG
```

## Testing

### Unit Tests
```java
@Test
public void testUploadFile() {
    // Test file upload
}

@Test
public void testDeleteMedia() {
    // Test media deletion
}
```

### Integration Tests
```java
@Test
public void testMediaUploadFlow() {
    // Test complete upload flow
}
```

## Future Enhancements

1. **Video Streaming**: Implement HLS/DASH streaming
2. **Image Processing**: Add filters and effects
3. **AI Integration**: Auto-tagging and content analysis
4. **Collaboration**: Shared media libraries
5. **Offline Support**: Local caching and sync 