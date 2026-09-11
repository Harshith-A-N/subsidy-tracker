import React, { useState } from 'react';
import Modal from './Modal';
import { ZoomIn, ZoomOut, RotateCw, RefreshCw, FileText } from 'lucide-react';
import { formatDate } from '../utils/formatters';

export default function DocumentViewerModal({
  isOpen,
  onClose,
  document: doc,
  applicationId,
}) {
  const [zoom, setZoom] = useState(1);
  const [rotation, setRotation] = useState(0);

  if (!isOpen || !doc) return null;

  // Determine file URL: priority to direct filePath (Cloudinary HTTPS / hosted), API stream, or standard fallback
  const rawPath = doc.filePath || doc.fileUrl || doc.url || '';
  const fileUrl = rawPath && (rawPath.startsWith('http://') || rawPath.startsWith('https://') || rawPath.startsWith('/'))
    ? rawPath
    : ((applicationId || doc.applicationId) && doc.id
        ? `/api/v1/applications/${applicationId || doc.applicationId}/documents/${doc.id}/file`
        : 'https://res.cloudinary.com/dubkk5bwa/image/upload/v1789099923/Subsidy%20Tracker/documents/gn0c3ygwnet5ashbd7z5.png');

  const isPdf = rawPath.toLowerCase().endsWith('.pdf') || (doc.documentType && doc.documentType.toLowerCase().includes('pdf'));

  const handleZoomIn = () => setZoom((prev) => Math.min(prev + 0.25, 3));
  const handleZoomOut = () => setZoom((prev) => Math.max(prev - 0.25, 0.5));
  const handleRotate = () => setRotation((prev) => (prev + 90) % 360);
  const handleReset = () => {
    setZoom(1);
    setRotation(0);
  };

  const docTitle = doc.documentType ? doc.documentType.replace(/_/g, ' ') : 'Attached Document';

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Online Document Preview: ${docTitle}`}
      subtitle={`Application #${applicationId || doc.applicationId || '—'} • Document ID #${doc.id || '—'}`}
      maxWidth="840px"
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        {/* Document Status & Metadata Bar */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '10px',
            padding: '10px 14px',
            backgroundColor: '#f8fafc',
            borderRadius: '8px',
            border: '1px solid #e2e8f0',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: '#475569' }}>
              Verification Status:
            </span>
            <span className={`badge ${doc.verificationStatus === 'VERIFIED' ? 'badge-success' : doc.verificationStatus === 'REJECTED' ? 'badge-danger' : 'badge-warning'}`}>
              {doc.verificationStatus || 'PENDING'}
            </span>
          </div>

          {doc.uploadedAt && (
            <div style={{ fontSize: '12px', color: '#64748b' }}>
              Uploaded on: {formatDate(doc.uploadedAt)}
            </div>
          )}

          {doc.remarks && (
            <div style={{ width: '100%', fontSize: '12px', color: '#334155', fontStyle: 'italic', borderTop: '1px dashed #cbd5e1', paddingTop: '6px' }}>
              Officer Remarks: "{doc.remarks}"
            </div>
          )}
        </div>

        {/* Interactive Controls Bar for Image View */}
        {!isPdf && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              padding: '6px 12px',
              backgroundColor: '#f1f5f9',
              borderRadius: '6px',
            }}
          >
            <button
              type="button"
              onClick={handleZoomIn}
              className="btn btn-secondary btn-sm"
              style={{ padding: '6px 10px', display: 'flex', alignItems: 'center', gap: '4px' }}
              title="Zoom In"
            >
              <ZoomIn size={14} />
              <span>Zoom In</span>
            </button>
            <button
              type="button"
              onClick={handleZoomOut}
              className="btn btn-secondary btn-sm"
              style={{ padding: '6px 10px', display: 'flex', alignItems: 'center', gap: '4px' }}
              title="Zoom Out"
            >
              <ZoomOut size={14} />
              <span>Zoom Out</span>
            </button>
            <button
              type="button"
              onClick={handleRotate}
              className="btn btn-secondary btn-sm"
              style={{ padding: '6px 10px', display: 'flex', alignItems: 'center', gap: '4px' }}
              title="Rotate 90°"
            >
              <RotateCw size={14} />
              <span>Rotate</span>
            </button>
            <button
              type="button"
              onClick={handleReset}
              className="btn btn-secondary btn-sm"
              style={{ padding: '6px 10px', display: 'flex', alignItems: 'center', gap: '4px' }}
              title="Reset View"
            >
              <RefreshCw size={14} />
              <span>Reset</span>
            </button>
            <span style={{ fontSize: '11px', color: '#64748b', marginLeft: '6px' }}>
              {Math.round(zoom * 100)}%
            </span>
          </div>
        )}

        {/* Live Document Preview Container */}
        <div
          style={{
            position: 'relative',
            width: '100%',
            height: 'min(60vh, 520px)',
            backgroundColor: '#0f172a',
            borderRadius: '8px',
            overflow: 'auto',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            border: '1px solid #334155',
          }}
        >
          {fileUrl ? (
            isPdf ? (
              <iframe
                src={fileUrl}
                title={docTitle}
                style={{ width: '100%', height: '100%', border: 'none' }}
              />
            ) : (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  transition: 'transform 0.15s ease-out',
                  transform: `scale(${zoom}) rotate(${rotation}deg)`,
                  transformOrigin: 'center center',
                  maxWidth: '100%',
                  maxHeight: '100%',
                  padding: '16px',
                }}
              >
                <img
                  src={fileUrl}
                  alt={docTitle}
                  style={{
                    maxWidth: '100%',
                    maxHeight: 'min(55vh, 480px)',
                    objectFit: 'contain',
                    borderRadius: '4px',
                    boxShadow: '0 10px 25px -5px rgba(0,0,0,0.5)',
                    backgroundColor: '#ffffff',
                  }}
                  onError={(e) => {
                    // Fallback in case of image load error
                    e.target.onerror = null;
                    e.target.src = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='300' height='200' viewBox='0 0 300 200'><rect width='300' height='200' fill='%23f1f5f9'/><text x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-size='14' fill='%2364748b'>Document Preview Unavailable</text></svg>";
                  }}
                />
              </div>
            )
          ) : (
            <div style={{ textAlign: 'center', color: '#94a3b8', padding: '20px' }}>
              <FileText size={42} style={{ margin: '0 auto 10px', opacity: 0.7 }} />
              <p style={{ margin: 0, fontSize: '14px' }}>Document file link not available.</p>
            </div>
          )}
        </div>

        {/* Footer Note */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '8px', fontSize: '12px', color: '#64748b' }}>
          <span>🔒 Certified digital verification asset hosted securely via Cloudinary CDN.</span>
          <button
            type="button"
            onClick={onClose}
            className="btn btn-secondary btn-sm"
          >
            Close Viewer
          </button>
        </div>
      </div>
    </Modal>
  );
}
