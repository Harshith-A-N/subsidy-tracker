import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { formatINR, formatINRFull, formatDate } from '../utils/formatters';
import Sidebar from '../components/Sidebar';
import MetricCard from '../components/MetricCard';
import StatusBadge from '../components/StatusBadge';
import SchemeDistributionChart from '../components/charts/SchemeDistributionChart';
import Modal from '../components/Modal';
import {
  Landmark,
  CheckCircle2,
  Clock,
  IndianRupee,
  PieChart,
  Send,
  Building,
  Layers,
  Calendar,
  Zap,
  ListChecks,
  Eye,
  Check,
  AlertTriangle,
  Menu,
  LogOut
} from 'lucide-react';

export default function FinanceApproverPortal() {
  const [activeTab, setActiveTab] = useState('disbursements');
  const [selectedApp, setSelectedApp] = useState(null);
  const [releaseModalOpen, setReleaseModalOpen] = useState(false);
  const [remarks, setRemarks] = useState('');

  // Tranche Schedule State
  const [scheduleModalOpen, setScheduleModalOpen] = useState(false);
  const [selectedScheduleApp, setSelectedScheduleApp] = useState(null);

  // Scheme Plan View State
  const [selectedSchemeId, setSelectedSchemeId] = useState(null);

  const { user, logout } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();

  // Finance Approval Queue Query (GET /api/v1/finance/queue)
  const { data: queue = [], isLoading } = useQuery({
    queryKey: ['finance-approval-queue'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/finance/queue');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Scheme Utilization Query (For Mandatory Chart #5)
  const { data: schemes = [] } = useQuery({
    queryKey: ['finance-scheme-utilization'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/analytics/fund-utilization/schemes');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // All Schemes list for Plan lookup
  const { data: allSchemes = [] } = useQuery({
    queryKey: ['finance-all-schemes'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/schemes');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Disbursement Schedule Query for selected app
  const { data: applicationSchedules = [], refetch: refetchAppSchedules } = useQuery({
    queryKey: ['finance-app-schedules', selectedScheduleApp?.id],
    queryFn: async () => {
      if (!selectedScheduleApp?.id) return [];
      const res = await apiClient.get(`/api/disbursement/schedules/application/${selectedScheduleApp.id}`);
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!selectedScheduleApp?.id && scheduleModalOpen,
  });

  // Disbursement Plan Query for selected scheme in Plans tab
  const activePlanSchemeId = selectedSchemeId || (allSchemes.length > 0 ? allSchemes[0].id : null);
  const { data: schemePlan } = useQuery({
    queryKey: ['finance-scheme-plan', activePlanSchemeId],
    queryFn: async () => {
      if (!activePlanSchemeId) return null;
      const res = await apiClient.get(`/api/disbursement/plans/scheme/${activePlanSchemeId}`);
      return res.success ? res.data : null;
    },
    enabled: !!activePlanSchemeId && activeTab === 'plans',
  });

  // Treasury Release Mutation (POST /api/v1/finance/{id}/review)
  const releaseMutation = useMutation({
    mutationFn: async ({ appId, payload }) => {
      return apiClient.post(`/api/v1/finance/${appId}/review`, payload);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success('Funds authorized for Direct Benefit Transfer (DBT) to beneficiary account!');
        queryClient.invalidateQueries(['finance-approval-queue']);
        queryClient.invalidateQueries(['finance-scheme-utilization']);
        setReleaseModalOpen(false);
        setRemarks('');
      } else {
        toast.error(res.error || 'Treasury authorization failed');
      }
    },
  });

  // Generate Schedule Mutation (POST /api/disbursement/schedules/generate/{id})
  const generateScheduleMutation = useMutation({
    mutationFn: async (appId) => {
      return apiClient.post(`/api/disbursement/schedules/generate/${appId}`);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success('Disbursement tranche schedule generated successfully!');
        refetchAppSchedules();
      } else {
        toast.error(res.error || 'Failed to generate schedule (check if scheme has a plan configured)');
      }
    },
  });

  // Release Single Schedule Tranche Stage (POST /api/disbursement/schedules/{id}/release)
  const releaseStageMutation = useMutation({
    mutationFn: async (scheduleId) => {
      return apiClient.post(`/api/disbursement/schedules/${scheduleId}/release`);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success('Stage tranche released via Treasury Direct Benefit Transfer!');
        refetchAppSchedules();
        queryClient.invalidateQueries(['finance-approval-queue']);
        queryClient.invalidateQueries(['finance-scheme-utilization']);
      } else {
        toast.error(res.error || 'Failed to release tranche stage');
      }
    },
  });

  // Create Milestones Mutation (POST /api/disbursement/compliance/application/{id})
  const createMilestonesMutation = useMutation({
    mutationFn: async (appId) => {
      return apiClient.post(`/api/disbursement/compliance/application/${appId}`);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success('Compliance milestones initialized for this application!');
      } else {
        toast.error(res.error || 'Failed to create milestones');
      }
    },
  });

  // Operational Overdue Milestones Query (GET /api/disbursement/compliance/overdue)
  const { data: overdueMilestones = [] } = useQuery({
    queryKey: ['finance-overdue-milestones'],
    queryFn: async () => {
      const res = await apiClient.get('/api/disbursement/compliance/overdue');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Operational Pending Milestones Query (GET /api/disbursement/compliance/pending)
  const { data: pendingMilestones = [] } = useQuery({
    queryKey: ['finance-pending-milestones'],
    queryFn: async () => {
      const res = await apiClient.get('/api/disbursement/compliance/pending');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Complete Compliance Milestone Mutation (PUT /api/disbursement/compliance/{milestoneId}/complete)
  const completeMilestoneMutation = useMutation({
    mutationFn: async (milestoneId) => {
      return apiClient.put(`/api/disbursement/compliance/${milestoneId}/complete`);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success('Compliance milestone verified & marked completed! Tranche release unlocked.');
        queryClient.invalidateQueries(['finance-overdue-milestones']);
        queryClient.invalidateQueries(['finance-pending-milestones']);
        queryClient.invalidateQueries(['finance-approval-queue']);
      } else {
        toast.error(res.error || 'Failed to complete milestone');
      }
    },
  });

  const handleReleaseSubmit = (e) => {
    e.preventDefault();
    if (!selectedApp) return;

    releaseMutation.mutate({
      appId: selectedApp.id,
      payload: {
        status: 'FINANCE_APPROVED',
        remarks: remarks || 'Funds release authorized to citizen bank account via DBT.',
      },
    });
  };

  const totalPendingAmount = queue.reduce((sum, a) => sum + (Number(a.requestedAmount) || 0), 0);

  const sidebarItems = [
    { type: 'heading', label: 'Treasury & Finance' },
    { key: 'disbursements', label: 'Disbursement Queue', icon: Landmark, badge: queue.length },
    { key: 'compliance', label: 'Compliance Milestones', icon: AlertTriangle, badge: overdueMilestones.length },
    { key: 'schedules', label: 'Tranche Management', icon: Calendar },
    { key: 'plans', label: 'Disbursement Plans', icon: Layers },
    { key: 'schemes', label: 'Scheme Budgets', icon: PieChart },
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
                Finance & Treasury Disbursement Portal 💰
              </h1>
              <p style={{ fontSize: '12.5px', color: '#64748b', marginTop: '3px' }}>
                Finance Officer: <strong>{user?.name}</strong> • Direct Benefit Transfer Authority & Treasury Integration
              </p>
            </div>
          </div>
          <button
            onClick={logout}
            className="btn btn-sm"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              color: '#dc2626',
              backgroundColor: '#fef2f2',
              border: '1px solid #fecaca',
              padding: '6px 12px',
              fontWeight: 600,
              fontSize: '12.5px',
              borderRadius: '8px',
              cursor: 'pointer',
              marginLeft: 'auto'
            }}
            title="Sign Out of Portal"
          >
            <LogOut size={15} />
            <span>Sign Out</span>
          </button>
        </div>

        {/* KPI Metrics */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 220px), 1fr))', gap: '16px', marginBottom: '24px' }}>
          <MetricCard
            title="Pending Fund Authorizations"
            value={queue.length}
            subtext="District-sanctioned applications"
            icon={Clock}
            color="warning"
          />
          <MetricCard
            title="Total Funds to Release"
            value={formatINR(totalPendingAmount)}
            subtext="Pending DBT authorization"
            icon={IndianRupee}
            color="primary"
          />
          <MetricCard
            title="Treasury Integration"
            value="Active"
            subtext="RBI / PFMS Gateway Live"
            icon={Building}
            color="success"
          />
        </div>

        {/* TAB 1: DISBURSEMENTS */}
        {activeTab === 'disbursements' && (
          <div className="grid-finance-disbursements">
            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                <h3 style={{ fontSize: '16px', fontWeight: 700 }}>
                  Awaiting Finance Release ({queue.length})
                </h3>
              </div>

              {queue.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px 12px', color: '#64748b' }}>
                  <CheckCircle2 size={36} color="#059669" style={{ margin: '0 auto 10px' }} />
                  <p style={{ fontWeight: 700 }}>All Treasury disbursements released!</p>
                  <p style={{ fontSize: '13px', marginTop: '4px' }}>No pending applications waiting for fund transfer.</p>
                </div>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>App ID</th>
                        <th>Beneficiary</th>
                        <th>Grant Amount</th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {queue.map((app) => (
                        <tr key={app.id}>
                          <td><code>#{app.id}</code></td>
                          <td>
                            <strong>{app.beneficiary?.fullName || 'Beneficiary'}</strong>
                            <div style={{ fontSize: '11px', color: '#64748b' }}>
                              {app.scheme?.schemeName || app.scheme?.name}
                            </div>
                          </td>
                          <td><strong>{formatINRFull(app.requestedAmount)}</strong></td>
                          <td><StatusBadge status={app.status} /></td>
                          <td>
                            <div style={{ display: 'flex', gap: '6px' }}>
                              <button
                                onClick={() => {
                                  setSelectedApp(app);
                                  setReleaseModalOpen(true);
                                }}
                                className="btn btn-success btn-sm"
                                title="Authorize Direct Benefit Transfer"
                              >
                                <Send size={13} />
                                <span>Release Funds</span>
                              </button>
                              <button
                                onClick={() => {
                                  setSelectedScheduleApp(app);
                                  setScheduleModalOpen(true);
                                }}
                                className="btn btn-outline btn-sm"
                                title="Manage Stage Tranches"
                              >
                                <Calendar size={13} />
                                <span>Tranches</span>
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Mandatory Chart #5: Scheme-Wise Budget Distribution */}
            <div className="card">
              <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '6px' }}>
                Scheme Budget Distribution
              </h3>
              <p style={{ fontSize: '12.5px', color: '#64748b', marginBottom: '16px' }}>
                Total utilized vs allocated funds across all active government schemes.
              </p>
              <div className="chart-wrapper">
                <SchemeDistributionChart schemes={schemes} />
              </div>
            </div>
          </div>
        )}

        {/* TAB: COMPLIANCE MILESTONES */}
        {activeTab === 'compliance' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                <div>
                  <h3 style={{ fontSize: '17px', fontWeight: 700, color: '#dc2626', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <AlertTriangle size={18} />
                    <span>Overdue Compliance Milestones ({overdueMilestones.length})</span>
                  </h3>
                  <p style={{ fontSize: '13px', color: '#64748b', marginTop: '2px' }}>
                    Critical milestones with passed due dates requiring immediate audit verification or extension.
                  </p>
                </div>
              </div>

              {overdueMilestones.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '32px 12px', color: '#059669', backgroundColor: '#f0fdf4', borderRadius: '8px' }}>
                  <CheckCircle2 size={32} style={{ margin: '0 auto 8px' }} />
                  <p style={{ fontWeight: 700 }}>No Overdue Milestones</p>
                  <p style={{ fontSize: '12.5px', marginTop: '2px' }}>All active staged grants are within their scheduled compliance windows.</p>
                </div>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Milestone ID</th>
                        <th>App ID</th>
                        <th>Stage Name</th>
                        <th>Scheduled Amount</th>
                        <th>Due Date</th>
                        <th>Status</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {overdueMilestones.map((ms) => (
                        <tr key={ms.id}>
                          <td><code>#{ms.id}</code></td>
                          <td><code>#{ms.applicationId || ms.application?.id}</code></td>
                          <td><strong>{ms.stageName || ms.stage?.stageName || 'Stage Verification'}</strong></td>
                          <td><strong>{formatINRFull(ms.scheduledAmount || 0)}</strong></td>
                          <td><span className="badge badge-danger">{formatDate(ms.dueDate)}</span></td>
                          <td><span className="badge badge-danger">OVERDUE</span></td>
                          <td>
                            <button
                              onClick={() => completeMilestoneMutation.mutate(ms.id)}
                              disabled={completeMilestoneMutation.isPending}
                              className="btn btn-success btn-sm"
                            >
                              <CheckCircle2 size={13} />
                              <span>Verify & Complete</span>
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                <div>
                  <h3 style={{ fontSize: '17px', fontWeight: 700 }}>
                    Active & Pending Compliance Milestones ({pendingMilestones.length})
                  </h3>
                  <p style={{ fontSize: '13px', color: '#64748b', marginTop: '2px' }}>
                    Milestones awaiting verification of ground proofs or utilization certificates before tranche release.
                  </p>
                </div>
              </div>

              {pendingMilestones.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '32px 12px', color: '#64748b', backgroundColor: '#f8fafc', borderRadius: '8px' }}>
                  <CheckCircle2 size={32} style={{ margin: '0 auto 8px', opacity: 0.6 }} />
                  <p style={{ fontWeight: 700 }}>No Pending Compliance Milestones</p>
                  <p style={{ fontSize: '12.5px', marginTop: '2px' }}>All required documentation and inspection proofs are cleared.</p>
                </div>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Milestone ID</th>
                        <th>App ID</th>
                        <th>Stage</th>
                        <th>Milestone Trigger</th>
                        <th>Scheduled Amount</th>
                        <th>Target Date</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pendingMilestones.map((ms) => (
                        <tr key={ms.id}>
                          <td><code>#{ms.id}</code></td>
                          <td><code>#{ms.applicationId || ms.application?.id}</code></td>
                          <td><strong>{ms.stageName || ms.stage?.stageName}</strong></td>
                          <td><span className="badge badge-primary">{ms.milestoneType || ms.triggerMilestone || 'GROUND_INSPECTION'}</span></td>
                          <td>{formatINRFull(ms.scheduledAmount || 0)}</td>
                          <td>{formatDate(ms.dueDate)}</td>
                          <td>
                            <button
                              onClick={() => completeMilestoneMutation.mutate(ms.id)}
                              disabled={completeMilestoneMutation.isPending}
                              className="btn btn-success btn-sm"
                            >
                              <CheckCircle2 size={13} />
                              <span>Verify & Complete</span>
                            </button>
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

        {/* TAB 2: TRANCHE MANAGEMENT */}
        {activeTab === 'schedules' && (
          <div className="card">
            <div style={{ marginBottom: '16px' }}>
              <h3 style={{ fontSize: '17px', fontWeight: 700 }}>
                Tranche Disbursement & Milestone Controls
              </h3>
              <p style={{ fontSize: '13px', color: '#64748b' }}>
                Manage milestone-based tranche releases for sanctioned applications.
              </p>
            </div>

            {queue.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '30px 12px', color: '#64748b' }}>
                <p>No applications currently awaiting tranche management in the queue.</p>
              </div>
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>App ID</th>
                      <th>Applicant</th>
                      <th>Scheme</th>
                      <th>Grant Amount</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {queue.map((app) => (
                      <tr key={app.id}>
                        <td><code>#{app.id}</code></td>
                        <td><strong>{app.beneficiary?.fullName || 'Beneficiary'}</strong></td>
                        <td>{app.scheme?.schemeName || app.scheme?.name}</td>
                        <td><strong>{formatINRFull(app.requestedAmount)}</strong></td>
                        <td>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button
                              onClick={() => {
                                setSelectedScheduleApp(app);
                                setScheduleModalOpen(true);
                              }}
                              className="btn btn-primary btn-sm"
                            >
                              <Eye size={13} />
                              <span>View / Release Tranches</span>
                            </button>
                            <button
                              onClick={() => generateScheduleMutation.mutate(app.id)}
                              disabled={generateScheduleMutation.isPending}
                              className="btn btn-secondary btn-sm"
                            >
                              <Zap size={13} />
                              <span>Generate Schedule</span>
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* TAB 3: DISBURSEMENT PLANS */}
        {activeTab === 'plans' && (
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '10px' }}>
              <div>
                <h3 style={{ fontSize: '17px', fontWeight: 700 }}>
                  Scheme Disbursement Plans
                </h3>
                <p style={{ fontSize: '13px', color: '#64748b' }}>
                  Inspection of configured staged tranche plans across all active programs.
                </p>
              </div>

              <select
                className="form-select"
                style={{ width: '260px' }}
                value={activePlanSchemeId || ''}
                onChange={(e) => setSelectedSchemeId(Number(e.target.value))}
              >
                {allSchemes.map((s) => (
                  <option key={s.id} value={s.id}>{s.name || s.schemeName}</option>
                ))}
              </select>
            </div>

            {schemePlan ? (
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
                  <span className="badge badge-success">Plan Active</span>
                  <span style={{ fontSize: '13px', color: '#64748b' }}>
                    {schemePlan.numberOfStages} Structured Stages • Created {formatDate(schemePlan.createdAt)}
                  </span>
                </div>

                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Sequence</th>
                        <th>Stage Name</th>
                        <th>Percentage</th>
                        <th>Milestone Trigger</th>
                        <th>Offset Days</th>
                      </tr>
                    </thead>
                    <tbody>
                      {schemePlan.stages?.map((st) => (
                        <tr key={st.id || st.sequenceNumber}>
                          <td><code>Stage {st.sequenceNumber}</code></td>
                          <td><strong>{st.stageName}</strong></td>
                          <td><span className="badge badge-primary">{st.percentageOfGrant}%</span></td>
                          <td><code>{st.triggerMilestone}</code></td>
                          <td>+{st.dueDateOffsetDays} days</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '40px 12px', color: '#64748b', backgroundColor: '#f8fafc', borderRadius: '8px' }}>
                <Layers size={36} style={{ margin: '0 auto 10px', opacity: 0.5 }} />
                <p style={{ fontWeight: 600 }}>No disbursement plan configured for this scheme yet.</p>
                <p style={{ fontSize: '12.5px', marginTop: '2px' }}>Disbursement plans are configured in the Admin Console.</p>
              </div>
            )}
          </div>
        )}

        {/* TAB 4: SCHEME BUDGETS */}
        {activeTab === 'schemes' && (
          <div className="card">
            <h3 style={{ fontSize: '17px', fontWeight: 700, marginBottom: '16px' }}>
              Scheme Budgetary Allocation & Utilization
            </h3>
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Scheme Name</th>
                    <th>Total Allocation</th>
                    <th>Utilized Budget</th>
                    <th>Remaining Allocation</th>
                    <th>Utilization Rate</th>
                  </tr>
                </thead>
                <tbody>
                  {schemes.map((s) => {
                    const total = Number(s.totalBudget) || 0;
                    const utilized = Number(s.utilizedBudget) || 0;
                    const remaining = Math.max(0, total - utilized);
                    const pct = s.utilizationPercent !== undefined ? s.utilizationPercent : (total > 0 ? (utilized / total) * 100 : 0);
                    return (
                      <tr key={s.schemeId || s.id || s.schemeName}>
                        <td><strong>{s.schemeName || s.name}</strong></td>
                        <td><strong>{formatINR(total)}</strong></td>
                        <td><span style={{ color: '#059669', fontWeight: 700 }}>{formatINR(utilized)}</span></td>
                        <td><span style={{ color: '#64748b' }}>{formatINR(remaining)}</span></td>
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <div style={{ flex: 1, height: '6px', backgroundColor: '#e2e8f0', borderRadius: '3px', overflow: 'hidden', minWidth: '60px' }}>
                              <div style={{
                                width: `${Math.min(100, pct)}%`,
                                height: '100%',
                                backgroundColor: pct > 80 ? '#dc2626' : pct > 50 ? '#d97706' : '#059669',
                                borderRadius: '3px',
                              }} />
                            </div>
                            <span style={{ fontSize: '12px', fontWeight: 700, color: '#334155' }}>
                              {Number(pct).toFixed(1)}%
                            </span>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </main>

      {/* Release Treasury Funds Modal */}
      <Modal
        isOpen={releaseModalOpen}
        onClose={() => setReleaseModalOpen(false)}
        title={`Authorize Treasury Release for Application #${selectedApp?.id}`}
        subtitle={`Beneficiary: ${selectedApp?.beneficiary?.fullName} • Grant: ${formatINRFull(selectedApp?.requestedAmount)}`}
      >
        <form onSubmit={handleReleaseSubmit}>
          <div style={{ backgroundColor: '#f0fdf4', border: '1px solid #bbf7d0', padding: '16px', borderRadius: '8px', marginBottom: '16px' }}>
            <div style={{ fontSize: '13px', fontWeight: 700, color: '#15803d', marginBottom: '4px' }}>
              ✓ Direct Benefit Transfer (DBT) Pre-Flight Check
            </div>
            <p style={{ fontSize: '12.5px', color: '#166534', margin: 0 }}>
              Sanction order verified. Funds will be directly debited from the treasury account and credited to the citizen's Aadhaar-linked bank account.
            </p>
          </div>

          <div className="form-group">
            <label className="form-label">Treasury Authorization Remarks & Reference Number</label>
            <textarea
              rows={3}
              placeholder="DBT payment batch authorized under PFMS treasury sanction order..."
              className="form-textarea"
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          </div>

          <button
            type="submit"
            disabled={releaseMutation.isPending}
            className="btn btn-success btn-lg"
            style={{ width: '100%', justifyContent: 'center', marginTop: '10px' }}
          >
            {releaseMutation.isPending ? 'Releasing Funds...' : 'Authorize Direct Treasury Release →'}
          </button>
        </form>
      </Modal>

      {/* Tranche Schedule Modal */}
      <Modal
        isOpen={scheduleModalOpen}
        onClose={() => setScheduleModalOpen(false)}
        title={`Tranche Disbursement Schedule (App #${selectedScheduleApp?.id})`}
        subtitle={`Applicant: ${selectedScheduleApp?.beneficiary?.fullName || 'Citizen'} • Scheme: ${selectedScheduleApp?.scheme?.schemeName || selectedScheduleApp?.scheme?.name}`}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
            <span style={{ fontSize: '13px', color: '#64748b' }}>
              Tranches are scheduled based on scheme disbursement plan and released sequentially.
            </span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                onClick={() => generateScheduleMutation.mutate(selectedScheduleApp?.id)}
                disabled={generateScheduleMutation.isPending}
                className="btn btn-secondary btn-sm"
              >
                <Zap size={13} />
                <span>Generate Schedule</span>
              </button>
              <button
                type="button"
                onClick={() => createMilestonesMutation.mutate(selectedScheduleApp?.id)}
                disabled={createMilestonesMutation.isPending}
                className="btn btn-outline btn-sm"
              >
                <ListChecks size={13} />
                <span>Init Milestones</span>
              </button>
            </div>
          </div>

          {applicationSchedules.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '30px 12px', backgroundColor: '#f8fafc', borderRadius: '8px', color: '#64748b' }}>
              <Calendar size={32} style={{ margin: '0 auto 8px', opacity: 0.5 }} />
              <p style={{ fontWeight: 600 }}>No schedule entries found</p>
              <p style={{ fontSize: '12px', marginTop: '2px' }}>Click "Generate Schedule" to create tranches for this application.</p>
            </div>
          ) : (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Tranche</th>
                    <th>Stage Name</th>
                    <th>Grant Amount</th>
                    <th>Target Date</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {applicationSchedules.map((sch) => (
                    <tr key={sch.id}>
                      <td><code>Stage {sch.stageSequenceNumber}</code></td>
                      <td><strong>{sch.stageName}</strong></td>
                      <td><strong style={{ color: '#059669' }}>{formatINRFull(sch.scheduledAmount)}</strong></td>
                      <td>{formatDate(sch.dueDate)}</td>
                      <td>
                        <span className={`badge ${sch.status === 'RELEASED' ? 'badge-success' : 'badge-warning'}`}>
                          {sch.status === 'RELEASED' ? '✓ Released' : 'Scheduled'}
                        </span>
                      </td>
                      <td>
                        {sch.status !== 'RELEASED' ? (
                          <button
                            type="button"
                            onClick={() => releaseStageMutation.mutate(sch.id)}
                            disabled={releaseStageMutation.isPending}
                            className="btn btn-success btn-sm"
                          >
                            <Send size={12} />
                            <span>Release Stage</span>
                          </button>
                        ) : (
                          <span style={{ fontSize: '12px', color: '#059669', fontWeight: 600 }}>Disbursed</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}
