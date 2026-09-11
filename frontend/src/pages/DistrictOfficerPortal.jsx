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
  FileText,
  CheckCircle2,
  XCircle,
  Clock,
  Building,
  FileCheck,
  Eye,
  History,
  Search,
  RefreshCw,
  Menu
} from 'lucide-react';

export default function DistrictOfficerPortal() {
  const [activeTab, setActiveTab] = useState('queue');
  const [selectedApp, setSelectedApp] = useState(null);
  const [sanctionModalOpen, setSanctionModalOpen] = useState(false);
  const [previewDoc, setPreviewDoc] = useState(null);
  const [decision, setDecision] = useState('APPROVE'); // 'APPROVE' | 'REJECT'
  const [remarks, setRemarks] = useState('');
  const [auditSearchTerm, setAuditSearchTerm] = useState('');

  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();

  // District Review Queue Query (GET /api/v1/district-officer/queue)
  const { data: queue = [], isLoading } = useQuery({
    queryKey: ['district-review-queue'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/district-officer/queue');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // District Audit Logs Query (GET /api/v1/audit-logs)
  const { data: auditLogs = [], isLoading: auditLogsLoading, refetch: refetchAuditLogs } = useQuery({
    queryKey: ['district-audit-logs'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/audit-logs');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: activeTab === 'audit-logs',
  });

  // Audit history for selected application
  const { data: appAuditHistory = [], isLoading: appAuditLoading } = useQuery({
    queryKey: ['district-app-audit', selectedApp?.id],
    queryFn: async () => {
      if (!selectedApp?.id) return [];
      const res = await apiClient.get(`/api/v1/audit-logs/application/${selectedApp.id}`);
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!selectedApp?.id && sanctionModalOpen,
  });

  // Sanctioned Orders List Query (GET /api/v1/applications/status/DISTRICT_APPROVED)
  const { data: sanctionOrders = [] } = useQuery({
    queryKey: ['district-sanction-orders'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/applications/status/DISTRICT_APPROVED');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
    enabled: activeTab === 'orders',
  });

  // Query documents for selected application in sanction modal (GET /api/v1/applications/{appId}/documents)
  const { data: appDocuments = [], isLoading: docsLoading } = useQuery({
    queryKey: ['district-app-docs', selectedApp?.id],
    queryFn: async () => {
      if (!selectedApp?.id) return [];
      const res = await apiClient.get(`/api/v1/applications/${selectedApp.id}/documents`);
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!selectedApp?.id && sanctionModalOpen,
  });

  // Sanction Decision Mutation (POST /api/v1/district-officer/{appId}/review)
  const decisionMutation = useMutation({
    mutationFn: async ({ appId, payload }) => {
      return apiClient.post(`/api/v1/district-officer/${appId}/review`, payload);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success(`District sanction order processed: ${decision}!`);
        queryClient.invalidateQueries(['district-review-queue']);
        queryClient.invalidateQueries(['district-sanction-orders']);
        setSanctionModalOpen(false);
        setRemarks('');
      } else {
        toast.error(res.error || 'Sanction action failed');
      }
    },
  });

  const handleDecisionSubmit = (e) => {
    e.preventDefault();
    if (!selectedApp) return;

    decisionMutation.mutate({
      appId: selectedApp.id,
      payload: {
        status: decision === 'APPROVE' ? 'DISTRICT_APPROVED' : 'DISTRICT_REJECTED',
        remarks,
      },
    });
  };

  const filteredAuditLogs = auditLogs.filter((log) => {
    if (!auditSearchTerm) return true;
    const term = auditSearchTerm.toLowerCase();
    return (
      String(log.id).includes(term) ||
      String(log.entityId).includes(term) ||
      (log.entityName || '').toLowerCase().includes(term) ||
      (log.action || '').toLowerCase().includes(term) ||
      (log.actorName || '').toLowerCase().includes(term) ||
      (log.details || '').toLowerCase().includes(term)
    );
  });

  const sidebarItems = [
    { type: 'heading', label: 'District Administration' },
    { key: 'queue', label: 'Sanction Approval Queue', icon: FileText, badge: queue.length },
    { key: 'orders', label: 'Sanction Orders', icon: FileCheck, badge: sanctionOrders.length },
    { key: 'audit-logs', label: 'Audit Trail & Decisions', icon: History, badge: auditLogs.length },
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
                {activeTab === 'queue' && 'District Officer Sanctioning Desk 📋'}
                {activeTab === 'orders' && 'Official District Sanction Orders 📜'}
                {activeTab === 'audit-logs' && 'District Audit Trail & Decision Logs 🔍'}
              </h1>
              <p style={{ fontSize: '12.5px', color: '#64748b', marginTop: '3px' }}>
                Officer: <strong>{user?.name}</strong> • Jurisdiction: <span className="badge badge-warning">{user?.region || 'District 1'}</span>
              </p>
            </div>
          </div>
        </div>

        {/* KPI Summary */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 220px), 1fr))', gap: '16px', marginBottom: '24px' }}>
          <MetricCard
            title="Pending District Approvals"
            value={queue.length}
            subtext="Field verified applications"
            icon={Clock}
            color="warning"
          />
          <MetricCard
            title="District Jurisdiction"
            value={user?.region || 'District 1'}
            subtext="Sanctioning Authority"
            icon={Building}
            color="primary"
          />
        </div>

        {/* TAB 1: SANCTION QUEUE */}
        {activeTab === 'queue' && (
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 700 }}>
                Sanction Review Queue ({queue.length})
              </h3>
            </div>

            {queue.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 12px', color: '#64748b' }}>
                <CheckCircle2 size={36} color="#059669" style={{ margin: '0 auto 10px' }} />
                <p style={{ fontWeight: 700 }}>All district sanctions up to date!</p>
                <p style={{ fontSize: '13px', marginTop: '4px' }}>No field-approved applications awaiting your sanction order.</p>
              </div>
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>App ID</th>
                      <th>Beneficiary</th>
                      <th>Scheme</th>
                      <th>Amount</th>
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
                            Region: {app.beneficiary?.region || '—'}
                          </div>
                        </td>
                        <td>{app.scheme?.schemeName || app.scheme?.name || 'Grant'}</td>
                        <td><strong>{formatINRFull(app.requestedAmount)}</strong></td>
                        <td><StatusBadge status={app.status} /></td>
                        <td>
                          <button
                            onClick={() => {
                              setSelectedApp(app);
                              setDecision('APPROVE');
                              setSanctionModalOpen(true);
                            }}
                            className="btn btn-primary btn-sm"
                          >
                            <FileCheck size={14} />
                            <span>Sanction Review</span>
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

        {/* TAB 2: SANCTION ORDERS */}
        {activeTab === 'orders' && (
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 700 }}>
                Issued Sanction Orders ({sanctionOrders.length})
              </h3>
            </div>

            {sanctionOrders.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 12px', color: '#64748b' }}>
                <FileCheck size={36} style={{ margin: '0 auto 10px', opacity: 0.5 }} />
                <p style={{ fontWeight: 700 }}>No sanction orders issued yet.</p>
              </div>
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Sanction ID</th>
                      <th>Beneficiary</th>
                      <th>Scheme</th>
                      <th>Sanctioned Amount</th>
                      <th>Status</th>
                      <th>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sanctionOrders.map((app) => (
                      <tr key={app.id}>
                        <td><code>#{app.id}</code></td>
                        <td><strong>{app.beneficiary?.fullName || 'Citizen'}</strong></td>
                        <td>{app.scheme?.schemeName || app.scheme?.name}</td>
                        <td><strong style={{ color: '#059669' }}>{formatINRFull(app.requestedAmount)}</strong></td>
                        <td><span className="badge badge-success">✓ District Sanctioned</span></td>
                        <td>{formatDate(app.updatedAt || app.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* TAB 3: AUDIT TRAIL & DECISION LOGS */}
        {activeTab === 'audit-logs' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div className="card" style={{ padding: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
                <div>
                  <h3 style={{ fontSize: '18px', fontWeight: 700, color: '#0f172a' }}>
                    District Decision History & State Transitions
                  </h3>
                  <p style={{ fontSize: '13px', color: '#64748b', marginTop: '2px' }}>
                    Real-time audit log of application lifecycle decisions, field inspections, document checks, and sanction orders.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => refetchAuditLogs()}
                  className="btn btn-secondary btn-sm"
                  style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                >
                  <RefreshCw size={14} className={auditLogsLoading ? 'spin' : ''} />
                  <span>Refresh Log</span>
                </button>
              </div>

              <div style={{ position: 'relative', maxWidth: '400px' }}>
                <Search size={15} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
                <input
                  type="text"
                  className="form-input"
                  placeholder="Search decisions by app ID, action, officer..."
                  value={auditSearchTerm}
                  onChange={(e) => setAuditSearchTerm(e.target.value)}
                  style={{ paddingLeft: '32px', fontSize: '13px' }}
                />
              </div>
            </div>

            <div className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                <h4 style={{ fontSize: '15px', fontWeight: 700 }}>
                  Recorded Events ({filteredAuditLogs.length})
                </h4>
              </div>

              {auditLogsLoading ? (
                <div style={{ padding: '32px', textAlign: 'center', color: '#64748b' }}>
                  Loading decision logs...
                </div>
              ) : filteredAuditLogs.length === 0 ? (
                <div style={{ padding: '32px', textAlign: 'center', color: '#64748b' }}>
                  No decision logs found.
                </div>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Timestamp</th>
                        <th>Action</th>
                        <th>Entity</th>
                        <th>Deciding Officer / Actor</th>
                        <th>Order Details & Remarks</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredAuditLogs.map((log) => (
                        <tr key={log.id}>
                          <td><code>#{log.id}</code></td>
                          <td style={{ whiteSpace: 'nowrap', fontSize: '12px', color: '#64748b' }}>
                            {formatDateTime(log.timestamp)}
                          </td>
                          <td>
                            <span className={`badge ${
                              log.action?.includes('APPROV') || log.action?.includes('VERIF')
                                ? 'badge-success'
                                : log.action?.includes('REJECT')
                                ? 'badge-danger'
                                : 'badge-primary'
                            }`}>
                              {log.action}
                            </span>
                          </td>
                          <td>
                            <span style={{ fontSize: '12.5px', fontWeight: 600 }}>{log.entityName}</span>
                            <span style={{ fontSize: '11px', color: '#64748b', marginLeft: '4px' }}>#{log.entityId}</span>
                          </td>
                          <td>
                            <div style={{ display: 'flex', flexDirection: 'column' }}>
                              <span style={{ fontSize: '13px', fontWeight: 600 }}>{log.actorName || 'System'}</span>
                              <span style={{ fontSize: '11px', color: '#64748b' }}>{log.actorRole || 'SYSTEM'}</span>
                            </div>
                          </td>
                          <td style={{ maxWidth: '380px' }}>
                            <div style={{ fontSize: '12.5px', color: '#334155', wordBreak: 'break-word' }}>
                              {log.details}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}
      </main>

      {/* Sanction Decision Modal */}
      <Modal
        isOpen={sanctionModalOpen}
        onClose={() => setSanctionModalOpen(false)}
        title={`Sanction Order for Application #${selectedApp?.id}`}
        subtitle={`Applicant: ${selectedApp?.beneficiary?.fullName} • Sanction Amount: ${formatINRFull(selectedApp?.requestedAmount)}`}
      >
        <form onSubmit={handleDecisionSubmit}>
          {/* Attached KYC Documents View */}
          <div style={{ marginBottom: '18px', padding: '14px', backgroundColor: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
            <h4 style={{ fontSize: '13.5px', fontWeight: 700, marginBottom: '8px' }}>
              Verified KYC Documents ({appDocuments.length})
            </h4>
            {docsLoading ? (
              <p style={{ fontSize: '12px' }}>Loading attached documents...</p>
            ) : appDocuments.length === 0 ? (
              <p style={{ fontSize: '12px', color: '#64748b' }}>No documents attached.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {appDocuments.map((doc) => (
                  <div key={doc.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 12px', backgroundColor: '#fff', borderRadius: '6px', border: '1px solid #e2e8f0' }}>
                    <div>
                      <span style={{ fontSize: '13px', fontWeight: 600 }}>{doc.documentType?.replace('_', ' ')}</span>
                      <span className={`badge ${doc.verificationStatus === 'VERIFIED' ? 'badge-success' : 'badge-warning'}`} style={{ marginLeft: '8px' }}>
                        {doc.verificationStatus || 'PENDING'}
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setPreviewDoc(doc)}
                      className="btn btn-secondary btn-sm"
                      style={{ display: 'flex', alignItems: 'center', gap: '4px' }}
                    >
                      <Eye size={13} />
                      <span>View Online</span>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Application Decision History & Audit Trail */}
          <div style={{ marginBottom: '18px', padding: '14px', backgroundColor: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
            <h4 style={{ fontSize: '13.5px', fontWeight: 700, marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <History size={15} color="#475569" />
              <span>Application Audit Trail & Timeline ({appAuditHistory.length})</span>
            </h4>
            {appAuditLoading ? (
              <p style={{ fontSize: '12px', color: '#64748b' }}>Loading decision timeline...</p>
            ) : appAuditHistory.length === 0 ? (
              <p style={{ fontSize: '12px', color: '#64748b' }}>No prior audit entries found for this application.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', maxHeight: '160px', overflowY: 'auto' }}>
                {appAuditHistory.map((entry) => (
                  <div key={entry.id} style={{ padding: '6px 10px', backgroundColor: '#fff', borderRadius: '6px', border: '1px solid #e2e8f0', fontSize: '12px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span className={`badge ${entry.action?.includes('APPROV') || entry.action?.includes('VERIF') ? 'badge-success' : entry.action?.includes('REJECT') ? 'badge-danger' : 'badge-primary'}`} style={{ fontSize: '10.5px' }}>
                        {entry.action}
                      </span>
                      <span style={{ color: '#64748b', fontSize: '11px' }}>{formatDateTime(entry.timestamp)}</span>
                    </div>
                    <div style={{ color: '#334155', marginTop: '2px', fontSize: '11.5px' }}>{entry.details}</div>
                    <div style={{ color: '#64748b', fontSize: '10.5px', marginTop: '1px' }}>
                      By: <strong>{entry.actorName || 'System'}</strong> ({entry.actorRole || 'SYSTEM'})
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="form-group">
            <label className="form-label">Sanction Decision <span className="req">*</span></label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <button
                type="button"
                onClick={() => setDecision('APPROVE')}
                className={`btn ${decision === 'APPROVE' ? 'btn-success' : 'btn-secondary'} btn-lg`}
                style={{ justifyContent: 'center' }}
              >
                <CheckCircle2 size={18} />
                <span>Issue Sanction (Approve)</span>
              </button>
              <button
                type="button"
                onClick={() => setDecision('REJECT')}
                className={`btn ${decision === 'REJECT' ? 'btn-danger' : 'btn-secondary'} btn-lg`}
                style={{ justifyContent: 'center' }}
              >
                <XCircle size={18} />
                <span>Reject Sanction</span>
              </button>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Official Order Remarks & Grant Allocation Details <span className="req">*</span></label>
            <textarea
              required
              rows={3}
              placeholder="Sanction order issued in compliance with regional grant guidelines..."
              className="form-textarea"
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          </div>

          <button
            type="submit"
            disabled={decisionMutation.isPending}
            className={`btn ${decision === 'APPROVE' ? 'btn-success' : 'btn-danger'} btn-lg`}
            style={{ width: '100%', justifyContent: 'center', marginTop: '10px' }}
          >
            {decisionMutation.isPending ? 'Processing Sanction Order...' : `Confirm ${decision} Order →`}
          </button>
        </form>
      </Modal>

      {/* Online Document Viewer Modal */}
      <DocumentViewerModal
        isOpen={!!previewDoc}
        onClose={() => setPreviewDoc(null)}
        document={previewDoc}
        applicationId={selectedApp?.id}
      />
    </div>
  );
}
