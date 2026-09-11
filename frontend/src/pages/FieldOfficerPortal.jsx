import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { formatINRFull, formatDate, formatDateTime } from '../utils/formatters';
import Sidebar from '../components/Sidebar';
import MetricCard from '../components/MetricCard';
import StatusBadge from '../components/StatusBadge';
import Modal from '../components/Modal';
import DocumentViewerModal from '../components/DocumentViewerModal';
import {
  Search,
  CheckCircle2,
  XCircle,
  Clock,
  MapPin,
  FileCheck,
  RotateCcw,
  FileText,
  Eye,
  Menu
} from 'lucide-react';

export default function FieldOfficerPortal() {
  const [activeTab, setActiveTab] = useState('queue');
  const [selectedApp, setSelectedApp] = useState(null);
  const [verifyModalOpen, setVerifyModalOpen] = useState(false);
  const [previewDoc, setPreviewDoc] = useState(null);
  const [actionType, setActionType] = useState('APPROVE'); // 'APPROVE' | 'REJECT' | 'RE_VERIFY'
  const [remarks, setRemarks] = useState('');
  const [gpsLocation, setGpsLocation] = useState('18.5204° N, 73.8567° E');

  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();

  // Verification Queue Query (GET /api/v1/field-verification/queue)
  const { data: queue = [], isLoading } = useQuery({
    queryKey: ['field-verification-queue'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/field-verification/queue');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Approved Ground Reports Query (GET /api/v1/applications/status/FIELD_APPROVED)
  const { data: approvedReports = [] } = useQuery({
    queryKey: ['field-approved-reports'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/applications/status/FIELD_APPROVED');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
    enabled: activeTab === 'reports',
  });

  // Query KYC documents for the currently inspected application (GET /api/v1/applications/{id}/documents)
  const { data: inspectionDocs = [], isLoading: docsLoading, refetch: refetchDocs } = useQuery({
    queryKey: ['field-inspection-docs', selectedApp?.id],
    queryFn: async () => {
      if (!selectedApp?.id) return [];
      const res = await apiClient.get(`/api/v1/applications/${selectedApp.id}/documents`);
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!selectedApp?.id && verifyModalOpen,
  });

  // Verify Single Document Mutation (PATCH /api/v1/applications/{appId}/documents/{docId}/verify)
  const verifyDocMutation = useMutation({
    mutationFn: async ({ docId, status, remarks }) => {
      return apiClient.patch(
        `/api/v1/applications/${selectedApp.id}/documents/${docId}/verify?status=${status}&remarks=${encodeURIComponent(remarks || '')}`
      );
    },
    onSuccess: (res, vars) => {
      if (res.success) {
        toast.success(`Document marked as ${vars.status}!`);
        refetchDocs();
      } else {
        toast.error(res.error || 'Failed to update document verification');
      }
    },
  });

  // Verify Application Mutation (POST /api/v1/field-verification/{appId}/verify)
  const verifyMutation = useMutation({
    mutationFn: async ({ appId, payload }) => {
      return apiClient.post(`/api/v1/field-verification/${appId}/verify`, payload);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success(`Application marked as ${actionType}!`);
        queryClient.invalidateQueries(['field-verification-queue']);
        queryClient.invalidateQueries(['field-approved-reports']);
        setVerifyModalOpen(false);
        setRemarks('');
      } else {
        toast.error(res.error || 'Verification action failed');
      }
    },
  });

  const handleVerifySubmit = (e) => {
    e.preventDefault();
    if (!selectedApp) return;

    verifyMutation.mutate({
      appId: selectedApp.id,
      payload: {
        status: actionType === 'APPROVE' ? 'FIELD_APPROVED' : actionType === 'REJECT' ? 'FIELD_REJECTED' : 'RE_VERIFICATION_REQUIRED',
        remarks,
        gpsLocation,
      },
    });
  };

  const sidebarItems = [
    { type: 'heading', label: 'Field Verification' },
    { key: 'queue', label: 'Inspection Queue', icon: Search, badge: queue.length },
    { key: 'reports', label: 'Ground Reports', icon: FileCheck, badge: approvedReports.length },
  ];

  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  return (
    <div className="portal-layout" style={{ backgroundColor: 'var(--bg-main)' }}>
      <Sidebar
        items={sidebarItems}
        activeTab={activeTab}
        onSelectTab={setActiveTab}
        isOpen={mobileNavOpen}
        onClose={() => setMobileNavOpen(false)}
      />

      <main className="portal-main fade-in">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', minWidth: 0 }}>
            <button
              onClick={() => setMobileNavOpen(true)}
              className="navbar-mobile-toggle"
              aria-label="Open Navigation Menu"
            >
              <Menu size={18} />
              <span style={{ fontSize: '12px', fontWeight: 600, marginLeft: '4px' }}>Menu</span>
            </button>
            <div style={{ minWidth: 0 }}>
              <h1 style={{ fontSize: 'clamp(18px, 2.2vw, 24px)', fontWeight: 800, color: '#0f172a', lineHeight: 1.2 }}>
                Field Inspection Workspace 🔍
              </h1>
              <p style={{ fontSize: '12.5px', color: '#64748b', marginTop: '3px' }}>
                Officer: <strong>{user?.name}</strong> • Region: <span className="badge badge-primary">{user?.region || 'District 1'}</span>
              </p>
            </div>
          </div>
        </div>

        {/* KPI Summary */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 220px), 1fr))', gap: '16px', marginBottom: '24px' }}>
          <MetricCard
            title="Pending Ground Inspections"
            value={queue.length}
            subtext="Applications requiring field visit"
            icon={Clock}
            color="warning"
          />
          <MetricCard
            title="Assigned Region"
            value={user?.region || 'District 1'}
            subtext="Geo-fenced jurisdiction"
            icon={MapPin}
            color="primary"
          />
        </div>

        {/* TAB 1: VERIFICATION QUEUE */}
        {activeTab === 'queue' && (
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 700 }}>
                Ground Inspection Queue ({queue.length})
              </h3>
            </div>

            {queue.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 12px', color: '#64748b' }}>
                <CheckCircle2 size={36} color="#059669" style={{ margin: '0 auto 10px' }} />
                <p style={{ fontWeight: 700 }}>All inspections completed!</p>
                <p style={{ fontSize: '13px', marginTop: '4px' }}>No pending field verification visits in your jurisdiction.</p>
              </div>
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>App ID</th>
                      <th>Beneficiary</th>
                      <th>Scheme Name</th>
                      <th>Requested Amount</th>
                      <th>Status</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {queue.map((app) => (
                      <tr key={app.id}>
                        <td><code>#{app.id}</code></td>
                        <td>
                          <strong>{app.beneficiary?.fullName || 'Beneficiary'}</strong>
                          <div style={{ fontSize: '11px', color: '#64748b' }}>
                            Aadhaar: •••• {(app.beneficiary?.nationalIdNumber || '').slice(-4)}
                          </div>
                        </td>
                        <td>{app.scheme?.schemeName || app.scheme?.name || 'Central Grant'}</td>
                        <td><strong>{formatINRFull(app.requestedAmount)}</strong></td>
                        <td><StatusBadge status={app.status} /></td>
                        <td>
                          <button
                            onClick={() => {
                              setSelectedApp(app);
                              setActionType('APPROVE');
                              setVerifyModalOpen(true);
                            }}
                            className="btn btn-primary btn-sm"
                          >
                            <FileCheck size={14} />
                            <span>Conduct Inspection</span>
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* TAB 2: GROUND REPORTS */}
        {activeTab === 'reports' && (
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 700 }}>
                Completed Ground Inspection Reports ({approvedReports.length})
              </h3>
            </div>

            {approvedReports.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 12px', color: '#64748b' }}>
                <FileText size={36} style={{ margin: '0 auto 10px', opacity: 0.5 }} />
                <p style={{ fontWeight: 700 }}>No completed inspections recorded yet.</p>
              </div>
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>App ID</th>
                      <th>Beneficiary</th>
                      <th>Scheme</th>
                      <th>Sanctioned Amount</th>
                      <th>Status</th>
                      <th>Verification Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {approvedReports.map((app) => (
                      <tr key={app.id}>
                        <td><code>#{app.id}</code></td>
                        <td><strong>{app.beneficiary?.fullName || 'Citizen'}</strong></td>
                        <td>{app.scheme?.schemeName || app.scheme?.name}</td>
                        <td><strong>{formatINRFull(app.requestedAmount)}</strong></td>
                        <td><span className="badge badge-success">✓ Field Verified</span></td>
                        <td>
                          {app.verificationDate
                            ? formatDateTime(app.verificationDate)
                            : app.submissionDate
                            ? `${formatDate(app.submissionDate)} (Submitted)`
                            : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </main>

      {/* Verification Action Modal */}
      <Modal
        isOpen={verifyModalOpen}
        onClose={() => setVerifyModalOpen(false)}
        title={`Ground Inspection for Application #${selectedApp?.id}`}
        subtitle={`Applicant: ${selectedApp?.beneficiary?.fullName || 'Beneficiary'} • Scheme: ${selectedApp?.scheme?.schemeName}`}
      >
        <form onSubmit={handleVerifySubmit}>
          {/* Section A: Attached KYC Documents Review */}
          <div style={{ marginBottom: '20px', backgroundColor: '#f8fafc', padding: '16px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
              <h4 style={{ fontSize: '14px', fontWeight: 700, margin: 0 }}>
                1. Prerequisite: Attached KYC & Land Documents ({inspectionDocs.length})
              </h4>
              <span style={{ fontSize: '11px', color: '#64748b' }}>
                Mandatory rule: Verify each document before issuing inspection approval
              </span>
            </div>

            {docsLoading ? (
              <p style={{ fontSize: '12px' }}>Loading attached documents...</p>
            ) : inspectionDocs.length === 0 ? (
              <div style={{ padding: '10px', backgroundColor: '#fff', borderRadius: '6px', fontSize: '12px', color: '#b45309' }}>
                ⚠️ No documents attached to this application.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {inspectionDocs.map((doc) => (
                  <div key={doc.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px', backgroundColor: '#fff', borderRadius: '6px', border: '1px solid #e2e8f0' }}>
                    <div>
                      <div style={{ fontSize: '13px', fontWeight: 700 }}>
                        {doc.documentType?.replace('_', ' ')}
                      </div>
                      <div style={{ fontSize: '11px', color: '#64748b', marginTop: '2px' }}>
                        Doc #{doc.id} • Status: <span className={`badge ${doc.verificationStatus === 'VERIFIED' ? 'badge-success' : doc.verificationStatus === 'REJECTED' ? 'badge-danger' : 'badge-warning'}`}>
                          {doc.verificationStatus || 'PENDING'}
                        </span>
                        {doc.verificationRemarks && ` • "${doc.verificationRemarks}"`}
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: '6px' }}>
                      <button
                        type="button"
                        onClick={() => setPreviewDoc(doc)}
                        className="btn btn-secondary btn-sm"
                        title="View Document Online"
                        style={{ display: 'flex', alignItems: 'center', gap: '4px' }}
                      >
                        <Eye size={13} />
                        <span>View Online</span>
                      </button>

                      {doc.verificationStatus !== 'VERIFIED' && (
                        <button
                          type="button"
                          onClick={() => verifyDocMutation.mutate({
                            docId: doc.id,
                            status: 'VERIFIED',
                            remarks: 'Document verified during field visit'
                          })}
                          disabled={verifyDocMutation.isPending}
                          className="btn btn-success btn-sm"
                          title="Mark Document as Verified"
                        >
                          <CheckCircle2 size={13} />
                          <span>Verify</span>
                        </button>
                      )}

                      {doc.verificationStatus !== 'REJECTED' && (
                        <button
                          type="button"
                          onClick={() => verifyDocMutation.mutate({
                            docId: doc.id,
                            status: 'REJECTED',
                            remarks: 'Document invalid or illegible during inspection'
                          })}
                          disabled={verifyDocMutation.isPending}
                          className="btn btn-danger btn-sm"
                          title="Reject Document"
                        >
                          <XCircle size={13} />
                          <span>Reject</span>
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Section B: Inspection Decision */}
          <div className="form-group">
            <label className="form-label">2. Inspection Outcome Decision <span className="req">*</span></label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px' }}>
              <button
                type="button"
                onClick={() => setActionType('APPROVE')}
                className={`btn ${actionType === 'APPROVE' ? 'btn-success' : 'btn-secondary'} btn-sm`}
                style={{ padding: '10px' }}
              >
                <CheckCircle2 size={16} />
                <span>Approve</span>
              </button>
              <button
                type="button"
                onClick={() => setActionType('RE_VERIFY')}
                className={`btn ${actionType === 'RE_VERIFY' ? 'btn-warning' : 'btn-secondary'} btn-sm`}
                style={{ padding: '10px' }}
              >
                <RotateCcw size={16} />
                <span>Re-Verify</span>
              </button>
              <button
                type="button"
                onClick={() => setActionType('REJECT')}
                className={`btn ${actionType === 'REJECT' ? 'btn-danger' : 'btn-secondary'} btn-sm`}
                style={{ padding: '10px' }}
              >
                <XCircle size={16} />
                <span>Reject</span>
              </button>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Geo-Coordinates / GPS Location</label>
            <input
              type="text"
              className="form-input"
              value={gpsLocation}
              onChange={(e) => setGpsLocation(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Field Inspection Remarks & Observations <span className="req">*</span></label>
            <textarea
              required
              rows={3}
              placeholder="Land verification confirmed, site inspected in person, assets verified..."
              className="form-textarea"
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          </div>

          <button
            type="submit"
            disabled={verifyMutation.isPending}
            className={`btn ${actionType === 'APPROVE' ? 'btn-success' : actionType === 'REJECT' ? 'btn-danger' : 'btn-warning'} btn-lg`}
            style={{ width: '100%', justifyContent: 'center', marginTop: '10px' }}
          >
            {verifyMutation.isPending ? 'Submitting...' : `Confirm ${actionType} →`}
          </button>
        </form>
      </Modal>

      {/* Online In-App Document Viewer Modal */}
      <DocumentViewerModal
        isOpen={!!previewDoc}
        onClose={() => setPreviewDoc(null)}
        document={previewDoc}
        applicationId={selectedApp?.id}
      />
    </div>
  );
}
