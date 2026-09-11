import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient, uploadDocument } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { formatINR, formatINRFull, formatDate } from '../utils/formatters';
import Sidebar from '../components/Sidebar';
import MetricCard from '../components/MetricCard';
import StatusBadge from '../components/StatusBadge';
import ApplicationStepper from '../components/ApplicationStepper';
import Modal from '../components/Modal';
import DocumentViewerModal from '../components/DocumentViewerModal';
import {
  LayoutDashboard,
  FilePlus,
  FolderOpen,
  IndianRupee,
  User,
  CheckCircle2,
  Clock,
  ArrowRight,
  Upload,
  ShieldCheck,
  AlertCircle,
  Edit3,
  Save,
  FileText,
  Eye,
  Send,
  ListChecks,
  Menu
} from 'lucide-react';
import { INDIAN_STATES } from '../utils/constants';

export default function BeneficiaryPortal() {
  const [activeTab, setActiveTab] = useState('overview');
  const [selectedScheme, setSelectedScheme] = useState(null);
  const [applyModalOpen, setApplyModalOpen] = useState(false);
  const [remarks, setRemarks] = useState('');
  const [selectedDocApp, setSelectedDocApp] = useState(null);
  const [uploadModalOpen, setUploadModalOpen] = useState(false);
  const [selectedFiles, setSelectedFiles] = useState({});
  const [uploadingDocType, setUploadingDocType] = useState(null);
  const [stageProofFile, setStageProofFile] = useState(null);
  const [uploadStageId, setUploadStageId] = useState('');
  const [previewDoc, setPreviewDoc] = useState(null);

  // Parity State: View Docs, View App Detail, Milestone Selection
  const [docsModalOpen, setDocsModalOpen] = useState(false);
  const [selectedDocsApp, setSelectedDocsApp] = useState(null);
  const [appDetailModalOpen, setAppDetailModalOpen] = useState(false);
  const [selectedAppId, setSelectedAppId] = useState(null);
  const [activeMilestoneAppId, setActiveMilestoneAppId] = useState(null);

  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();

  // Profile Edit State
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const [profileForm, setProfileForm] = useState({
    fullName: '',
    phoneNumber: '',
    address: '',
    category: 'GENERAL',
    region: 'Maharashtra',
    annualIncome: '',
  });

  const handleStartEditProfile = () => {
    setProfileForm({
      fullName: beneficiary?.fullName || user?.name || '',
      phoneNumber: beneficiary?.phoneNumber || '',
      address: beneficiary?.address || '',
      category: beneficiary?.category || 'GENERAL',
      region: beneficiary?.region || user?.region || 'Maharashtra',
      annualIncome: beneficiary?.annualIncome ? String(beneficiary.annualIncome) : '',
    });
    setIsEditingProfile(true);
  };

  const updateProfileMutation = useMutation({
    mutationFn: async (formData) => {
      const payload = {
        fullName: formData.fullName,
        phoneNumber: formData.phoneNumber,
        address: formData.address,
        category: formData.category,
        region: formData.region,
        annualIncome: parseFloat(formData.annualIncome) || 0,
      };
      if (beneficiary?.id) {
        return apiClient.put(`/api/v1/beneficiaries/${beneficiary.id}`, {
          ...payload,
          nationalIdNumber: beneficiary.nationalIdNumber,
        });
      } else {
        return apiClient.post('/api/v1/beneficiaries', {
          ...payload,
          nationalIdNumber: '123456789012',
        });
      }
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success('Citizen profile updated successfully!');
        queryClient.invalidateQueries(['beneficiary-profile']);
        setIsEditingProfile(false);
      } else {
        toast.error(res.error || 'Failed to update profile');
      }
    },
  });

  const handleSaveProfile = (e) => {
    e.preventDefault();
    updateProfileMutation.mutate(profileForm);
  };

  // Beneficiary Profile Query
  const { data: beneficiary, isLoading: profileLoading } = useQuery({
    queryKey: ['beneficiary-profile'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/beneficiaries/me');
      return res.success ? res.data : null;
    },
    staleTime: 5 * 60 * 1000,
  });

  // Beneficiary Applications Query
  const { data: applications = [], isLoading: appsLoading } = useQuery({
    queryKey: ['my-applications'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/applications/my-applications');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Schemes Query
  const { data: schemes = [] } = useQuery({
    queryKey: ['public-schemes'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/schemes');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Scheme Slabs Query for Selected Scheme
  const { data: selectedSchemeSlabs = [] } = useQuery({
    queryKey: ['scheme-slabs', selectedScheme?.id],
    queryFn: async () => {
      if (!selectedScheme?.id) return [];
      const res = await apiClient.get(`/api/v1/schemes/${selectedScheme.id}/slabs`);
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!selectedScheme?.id && applyModalOpen,
  });

  const getSchemeIndividualGrant = (scheme, userCategory) => {
    if (!scheme) return 50000;
    const cat = userCategory || beneficiary?.category || 'GENERAL';
    const slabs = (scheme.slabs && Array.isArray(scheme.slabs) && scheme.slabs.length > 0)
      ? scheme.slabs
      : (selectedScheme?.id === scheme.id ? selectedSchemeSlabs : []);

    if (slabs && slabs.length > 0) {
      const match = slabs.find((s) => s.category === cat);
      if (match && Number(match.grantAmount) > 0) return Number(match.grantAmount);
      const general = slabs.find((s) => s.category === 'GENERAL');
      if (general && Number(general.grantAmount) > 0) return Number(general.grantAmount);
      return Number(slabs[0].grantAmount) || 50000;
    }
    return Number(scheme.maxGrantAmount) || 50000;
  };

  const fixedGrantAmount = getSchemeIndividualGrant(selectedScheme, beneficiary?.category);

  // Create Application Mutation (Starts as DRAFT per README workflow)
  const submitAppMutation = useMutation({
    mutationFn: async (payload) => {
      return apiClient.post('/api/v1/applications', payload);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success(`Draft Application #${res.data?.id} created! Please attach required KYC documents and then click Submit.`);
        queryClient.invalidateQueries(['my-applications']);
        setApplyModalOpen(false);
        setActiveTab('applications');
      } else {
        toast.error(res.error || 'Failed to create application');
      }
    },
  });

  // Upload Document Mutation (with optional stageId for utilization proof)
  const uploadDocMutation = useMutation({
    mutationFn: async ({ appId, file, type, stageId }) => {
      const fd = new FormData();
      fd.append('file', file);
      const url = `/api/v1/applications/${appId}/documents?documentType=${encodeURIComponent(type)}${stageId ? `&stageId=${stageId}` : ''}`;
      return uploadDocument(url, fd);
    },
    onSuccess: (res, vars) => {
      if (res.success) {
        toast.success(`Uploaded "${vars.type}" successfully!`);
        queryClient.invalidateQueries(['my-applications']);
        queryClient.invalidateQueries(['beneficiary-app-documents']);
        // Clear selected file for this specific document type
        setSelectedFiles((prev) => {
          const next = { ...prev };
          delete next[vars.type];
          return next;
        });
        if (vars.type === 'STAGE_UTILIZATION_PROOF') {
          setStageProofFile(null);
          setUploadStageId('');
        }
      } else {
        toast.error(res.error || 'Failed to upload document');
      }
      setUploadingDocType(null);
    },
    onError: (err) => {
      toast.error(err.message || 'Upload failed');
      setUploadingDocType(null);
    }
  });

  const handleApplySubmit = (e) => {
    e.preventDefault();
    if (!selectedScheme) return;
    submitAppMutation.mutate({
      schemeId: selectedScheme.id,
      remarks,
    });
  };

  // Submit Draft Application Mutation (POST /api/v1/applications/{id}/submit)
  const submitDraftMutation = useMutation({
    mutationFn: async (appId) => {
      return apiClient.post(`/api/v1/applications/${appId}/submit`);
    },
    onSuccess: (res, appId) => {
      if (res.success) {
        toast.success(`Application #${res.data?.id || appId} submitted successfully! Status: ${res.data?.status}`);
        queryClient.invalidateQueries(['my-applications']);
      } else {
        toast.error(res.error || 'Failed to submit application');
        const targetApp = applications.find(a => a.id === appId);
        if (targetApp) {
          setSelectedDocApp(targetApp);
          setUploadModalOpen(true);
        }
      }
    },
    onError: (err, appId) => {
      toast.error(err.message || 'Failed to submit application');
      const targetApp = applications.find(a => a.id === appId);
      if (targetApp) {
        setSelectedDocApp(targetApp);
        setUploadModalOpen(true);
      }
    }
  });

  // Client-side Pre-Validation before submitting draft application
  const handleSubmitDraftApplication = async (app) => {
    try {
      // Check already loaded or fetch fresh documents for this app
      const res = await apiClient.get(`/api/v1/applications/${app.id}/documents`);
      const docs = res.success && Array.isArray(res.data) ? res.data : [];

      const appScheme = schemes.find(s => s.id === (app.scheme?.id || app.schemeId)) || app.scheme;
      const reqDocsStr = appScheme?.requiredDocuments || app.scheme?.requiredDocuments || 'Aadhaar Card, Land Record, Bank Passbook';
      const requiredDocs = reqDocsStr.split(',').map(s => s.trim()).filter(Boolean);

      const uploadedTypes = new Set(docs.map(d => (d.documentType || '').trim().toLowerCase()));
      const missing = requiredDocs.filter(req => !uploadedTypes.has(req.toLowerCase()));

      if (docs.length === 0 || missing.length > 0) {
        toast.error(`Cannot submit: Missing required document${missing.length > 1 ? 's' : ''}: ${missing.join(', ')}. Please upload required files first.`);
        setSelectedDocApp(app);
        setUploadModalOpen(true);
        return;
      }

      submitDraftMutation.mutate(app.id);
    } catch {
      submitDraftMutation.mutate(app.id);
    }
  };

  // Query documents for selected application (GET /api/v1/applications/{appId}/documents)
  const activeDocsAppId = selectedDocApp?.id || selectedDocsApp?.id;
  const { data: appDocuments = [], isLoading: docsLoading, refetch: refetchDocs } = useQuery({
    queryKey: ['beneficiary-app-documents', activeDocsAppId],
    queryFn: async () => {
      if (!activeDocsAppId) return [];
      const res = await apiClient.get(`/api/v1/applications/${activeDocsAppId}/documents`);
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!activeDocsAppId && (docsModalOpen || uploadModalOpen),
  });

  // Query single application detail (GET /api/v1/applications/{id})
  const { data: singleAppDetail, isLoading: singleAppLoading } = useQuery({
    queryKey: ['beneficiary-app-detail', selectedAppId],
    queryFn: async () => {
      if (!selectedAppId) return null;
      const res = await apiClient.get(`/api/v1/applications/${selectedAppId}`);
      return res.success ? res.data : null;
    },
    enabled: !!selectedAppId && appDetailModalOpen,
  });

  // Active app for milestones tab
  const currentMilestoneAppId = activeMilestoneAppId || (applications.length > 0 ? applications[0].id : null);

  // Query disbursement schedule for application (GET /api/disbursement/schedules/application/{id})
  const { data: appSchedules = [] } = useQuery({
    queryKey: ['beneficiary-app-schedules', currentMilestoneAppId],
    queryFn: async () => {
      if (!currentMilestoneAppId) return [];
      const res = await apiClient.get(`/api/disbursement/schedules/application/${currentMilestoneAppId}`);
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!currentMilestoneAppId && activeTab === 'milestones',
  });

  // Query compliance milestones for application (GET /api/disbursement/compliance/application/{id})
  const { data: appComplianceMilestones = [] } = useQuery({
    queryKey: ['beneficiary-app-compliance', currentMilestoneAppId],
    queryFn: async () => {
      if (!currentMilestoneAppId) return [];
      const res = await apiClient.get(`/api/disbursement/compliance/application/${currentMilestoneAppId}`);
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!currentMilestoneAppId && activeTab === 'milestones',
  });

  const sidebarItems = [
    { type: 'heading', label: 'Beneficiary Space' },
    { key: 'overview', label: 'My Dashboard', icon: LayoutDashboard },
    { key: 'apply', label: 'Apply for Scheme', icon: FilePlus },
    { key: 'applications', label: 'My Applications', icon: FolderOpen, badge: applications.length },
    { key: 'milestones', label: 'Funds & Milestones', icon: IndianRupee },
    { key: 'profile', label: 'My Profile', icon: User },
  ];

  const totalDisbursed = applications
    .filter((a) => a.status === 'DISBURSED' || a.status === 'COMPLETED')
    .reduce((sum, a) => sum + (Number(a.requestedAmount) || 0), 0);

  const pendingCount = applications.filter((a) => !['DISBURSED', 'COMPLETED', 'NOT_ELIGIBLE', 'FIELD_REJECTED', 'DISTRICT_REJECTED', 'FINANCE_REJECTED'].includes(a.status)).length;

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
        {/* Top Header */}
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
                {activeTab === 'overview' && 'Citizen Dashboard 🏠'}
                {activeTab === 'apply' && 'Apply for New Subsidy 📝'}
                {activeTab === 'applications' && 'My Applications & Status 📂'}
                {activeTab === 'milestones' && 'Milestones & Fund Transfers 💰'}
                {activeTab === 'profile' && 'Citizen Profile & Verification 👤'}
              </h1>
              <p style={{ fontSize: '12.5px', color: '#64748b', marginTop: '3px' }}>
                Welcome back, {beneficiary?.fullName || user?.name || 'Citizen'}. Manage your government grants and direct transfers.
              </p>
            </div>
          </div>

          {activeTab !== 'apply' && (
            <button
              onClick={() => setActiveTab('apply')}
              className="btn btn-primary btn-sm"
            >
              <FilePlus size={15} />
              <span>Apply for Scheme</span>
            </button>
          )}
        </div>

        {/* TAB 1: OVERVIEW */}
        {activeTab === 'overview' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            {/* KPI Cards */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
              <MetricCard
                title="Total Applications"
                value={applications.length}
                subtext="All schemes enrolled"
                icon={FolderOpen}
                color="primary"
              />
              <MetricCard
                title="Under Active Review"
                value={pendingCount}
                subtext="Field / District / Finance"
                icon={Clock}
                color="warning"
              />
              <MetricCard
                title="Funds Disbursed"
                value={formatINR(totalDisbursed)}
                subtext="Direct Benefit Transfer"
                icon={IndianRupee}
                color="success"
              />
              <MetricCard
                title="Aadhaar KYC Status"
                value={beneficiary ? 'Verified' : 'Pending'}
                subtext={beneficiary?.nationalIdNumber ? `ID: •••• ${beneficiary.nationalIdNumber.slice(-4)}` : 'Complete setup'}
                icon={ShieldCheck}
                color={beneficiary ? 'success' : 'warning'}
              />
            </div>

            {/* Recent Applications Preview */}
            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                <h3 style={{ fontSize: '16px', fontWeight: 700 }}>Recent Application Tracker</h3>
                <button onClick={() => setActiveTab('applications')} className="btn btn-ghost btn-sm">
                  <span>View All</span>
                  <ArrowRight size={14} />
                </button>
              </div>

              {applications.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '36px 12px', color: '#64748b' }}>
                  <FolderOpen size={36} color="#cbd5e1" style={{ margin: '0 auto 10px' }} />
                  <p style={{ fontWeight: 600 }}>No applications filed yet.</p>
                  <p style={{ fontSize: '13px', marginTop: '4px' }}>Explore available central and state schemes to apply for financial assistance.</p>
                  <button onClick={() => setActiveTab('apply')} className="btn btn-primary btn-sm" style={{ marginTop: '14px' }}>
                    Browse Schemes
                  </button>
                </div>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Application ID</th>
                        <th>Scheme</th>
                        <th>Applied Amount</th>
                        <th>Status</th>
                        <th>Applied On</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {applications.slice(0, 5).map((app) => (
                        <tr key={app.id}>
                          <td><strong>#{app.id}</strong></td>
                          <td>{app.scheme?.schemeName || 'Central Grant'}</td>
                          <td><strong>{formatINRFull(app.requestedAmount)}</strong></td>
                          <td><StatusBadge status={app.status} /></td>
                          <td>{formatDate(app.submissionDate || app.createdAt)}</td>
                          <td>
                            <button
                              onClick={() => { setSelectedDocApp(app); setUploadModalOpen(true); }}
                              className="btn btn-secondary btn-sm"
                            >
                              <Upload size={13} />
                              <span>Upload Docs</span>
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

        {/* TAB 2: APPLY FOR SCHEME */}
        {activeTab === 'apply' && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: '20px' }}>
            {schemes
              .filter((s) => s.isActive !== false && s.active !== false)
              .map((scheme) => {
                const individualGrant = getSchemeIndividualGrant(scheme, beneficiary?.category);
                return (
                  <div key={scheme.id} className="card card-hover" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
                        <span className="badge badge-primary">{beneficiary?.category || scheme.targetCategory || 'GENERAL'}</span>
                        <span style={{ fontSize: '12px', color: '#64748b', fontWeight: 600 }}>ID: #{scheme.id}</span>
                      </div>
                      <h3 style={{ fontSize: '17px', fontWeight: 700, marginBottom: '8px' }}>{scheme.name || scheme.schemeName}</h3>
                      <p style={{ fontSize: '13px', color: '#475569', lineHeight: 1.5, marginBottom: '16px' }}>
                        {scheme.description || 'Government assistance and grant support.'}
                      </p>
                    </div>

                    <div style={{ borderTop: '1px solid #f1f5f9', paddingTop: '14px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
                        <div>
                          <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <span>Individual Grant Amount</span>
                            <span className="badge badge-primary" style={{ fontSize: '10px', padding: '1px 6px' }}>
                              {beneficiary?.category || 'GENERAL'} Slab
                            </span>
                          </div>
                          <div style={{ fontSize: '18px', fontWeight: 800, color: '#059669', marginTop: '2px' }}>
                            {formatINRFull(individualGrant)}
                          </div>
                        </div>
                      </div>

                      <button
                        onClick={() => {
                          setSelectedScheme(scheme);
                          setRemarks('');
                          setApplyModalOpen(true);
                        }}
                        className="btn btn-primary btn-sm"
                        style={{ width: '100%', justifyContent: 'center' }}
                      >
                        <span>Apply Now</span>
                        <ArrowRight size={14} />
                      </button>
                    </div>
                  </div>
                );
              })}
          </div>
        )}

        {/* TAB 3: APPLICATIONS & STEPPER */}
        {activeTab === 'applications' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {applications.length === 0 ? (
              <div className="card" style={{ textAlign: 'center', padding: '48px 24px', color: '#64748b' }}>
                <FolderOpen size={48} style={{ margin: '0 auto 12px', opacity: 0.5 }} />
                <h3 style={{ fontSize: '18px', fontWeight: 700, color: '#0f172a' }}>No Applications Yet</h3>
                <p style={{ fontSize: '13px', marginTop: '4px' }}>Browse available schemes and apply for direct government subsidies.</p>
                <button
                  onClick={() => setActiveTab('apply')}
                  className="btn btn-primary btn-sm"
                  style={{ marginTop: '16px', marginInline: 'auto' }}
                >
                  <FilePlus size={14} />
                  <span>Browse Schemes</span>
                </button>
              </div>
            ) : (
              applications.map((app) => (
                <div key={app.id} className="card" style={{ padding: '24px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px', marginBottom: '16px' }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <h3 style={{ fontSize: '18px', fontWeight: 800 }}>{app.scheme?.schemeName || app.scheme?.name || 'Government Grant'}</h3>
                        <code style={{ fontSize: '12px', backgroundColor: '#f1f5f9', padding: '2px 8px', borderRadius: '4px' }}>
                          App #{app.id}
                        </code>
                      </div>
                      <div style={{ fontSize: '13px', color: '#64748b', marginTop: '4px' }}>
                        Applied for: <strong>{formatINRFull(app.requestedAmount)}</strong> • Date: {formatDate(app.submissionDate || app.createdAt)}
                      </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                      <StatusBadge status={app.status} />

                      {/* Draft Submission Action (POST /api/v1/applications/{id}/submit) */}
                      {app.status === 'DRAFT' && (
                        <button
                          onClick={() => handleSubmitDraftApplication(app)}
                          disabled={submitDraftMutation.isPending}
                          className="btn btn-success btn-sm"
                          title="Verify mandatory documents and submit application"
                        >
                          <Send size={13} />
                          <span>Submit Draft</span>
                        </button>
                      )}

                      {/* View Documents (GET /api/v1/applications/{id}/documents) */}
                      <button
                        onClick={() => { setSelectedDocsApp(app); setDocsModalOpen(true); }}
                        className="btn btn-outline btn-sm"
                      >
                        <FileText size={13} />
                        <span>View Docs</span>
                      </button>

                      {/* Attach New Documents (POST .../documents) */}
                      <button
                        onClick={() => { setSelectedDocApp(app); setUploadModalOpen(true); }}
                        className="btn btn-secondary btn-sm"
                      >
                        <Upload size={13} />
                        <span>Upload</span>
                      </button>

                      {/* View Application Full Details (GET /api/v1/applications/{id}) */}
                      <button
                        onClick={() => { setSelectedAppId(app.id); setAppDetailModalOpen(true); }}
                        className="btn btn-primary btn-sm"
                      >
                        <Eye size={13} />
                        <span>Details</span>
                      </button>
                    </div>
                  </div>

                  {/* Stepper Progress */}
                  <ApplicationStepper status={app.status} />
                </div>
              ))
            )}
          </div>
        )}

        {/* TAB 4: MILESTONES & FUNDS (WIRED TO REAL SCHEDULES & COMPLIANCE ENDPOINTS) */}
        {activeTab === 'milestones' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            {applications.length === 0 ? (
              <div className="card" style={{ textAlign: 'center', padding: '40px 12px', color: '#64748b' }}>
                <p>No active applications found. Submit an application to view disbursement schedules.</p>
              </div>
            ) : (
              <>
                {/* Application Selector */}
                {applications.length > 1 && (
                  <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                    {applications.map((a) => (
                      <button
                        key={a.id}
                        onClick={() => setActiveMilestoneAppId(a.id)}
                        className={`btn btn-sm ${currentMilestoneAppId === a.id ? 'btn-primary' : 'btn-outline'}`}
                      >
                        App #{a.id}: {a.scheme?.schemeName || a.scheme?.name || 'Grant'}
                      </button>
                    ))}
                  </div>
                )}

                {/* Section A: Tranche Disbursement Schedule */}
                <div className="card">
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                    <div>
                      <h3 style={{ fontSize: '17px', fontWeight: 700 }}>
                        Tranche Payment Schedule (App #{currentMilestoneAppId})
                      </h3>
                      <p style={{ fontSize: '12.5px', color: '#64748b' }}>
                        Treasury Direct Benefit Transfer (DBT) schedule released in milestone stages.
                      </p>
                    </div>
                    <span className="badge badge-primary">
                      🏦 Bank Transfer Direct (PFMS)
                    </span>
                  </div>

                  {appSchedules.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '30px 12px', color: '#64748b', backgroundColor: '#f8fafc', borderRadius: '8px' }}>
                      <Clock size={32} style={{ margin: '0 auto 8px', opacity: 0.5 }} />
                      <p style={{ fontWeight: 600 }}>Payment schedule pending generation</p>
                      <p style={{ fontSize: '12px', marginTop: '2px' }}>
                        Tranches will be generated once your application completes District Sanction review.
                      </p>
                    </div>
                  ) : (
                    <div className="table-container">
                      <table className="data-table">
                        <thead>
                          <tr>
                            <th>Tranche Stage</th>
                            <th>Stage Name</th>
                            <th>Scheduled Grant</th>
                            <th>Target Date</th>
                            <th>Payment Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          {appSchedules.map((sch) => (
                            <tr key={sch.id}>
                              <td><code>Stage {sch.stageSequenceNumber}</code></td>
                              <td><strong>{sch.stageName}</strong></td>
                              <td><strong style={{ color: '#059669' }}>{formatINRFull(sch.scheduledAmount)}</strong></td>
                              <td>{formatDate(sch.dueDate)}</td>
                              <td>
                                <span className={`badge ${sch.status === 'RELEASED' ? 'badge-success' : 'badge-warning'}`}>
                                  {sch.status === 'RELEASED' ? '✓ Disbursed to Bank' : 'Scheduled'}
                                </span>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>

                {/* Section B: Compliance Milestones */}
                <div className="card">
                  <div style={{ marginBottom: '16px' }}>
                    <h3 style={{ fontSize: '17px', fontWeight: 700 }}>
                      Compliance Milestones & Inspections (App #{currentMilestoneAppId})
                    </h3>
                    <p style={{ fontSize: '12.5px', color: '#64748b' }}>
                      Physical verification milestones required by field officers to release subsequent tranches.
                    </p>
                  </div>

                  {appComplianceMilestones.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '30px 12px', color: '#64748b', backgroundColor: '#f8fafc', borderRadius: '8px' }}>
                      <ListChecks size={32} style={{ margin: '0 auto 8px', opacity: 0.5 }} />
                      <p style={{ fontWeight: 600 }}>No compliance milestones registered yet</p>
                      <p style={{ fontSize: '12px', marginTop: '2px' }}>Milestones are scheduled once the scheme disbursement plan activates.</p>
                    </div>
                  ) : (
                    <div className="table-container">
                      <table className="data-table">
                        <thead>
                          <tr>
                            <th>Milestone</th>
                            <th>Milestone Trigger Condition</th>
                            <th>Target Due Date</th>
                            <th>Verification Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          {appComplianceMilestones.map((m) => (
                            <tr key={m.id}>
                              <td><code>#{m.id}</code></td>
                              <td><strong>{m.triggerMilestone || m.stageName || 'Field Inspection'}</strong></td>
                              <td>{formatDate(m.dueDate)}</td>
                              <td>
                                <span className={`badge ${m.status === 'COMPLETED' ? 'badge-success' : m.status === 'OVERDUE' ? 'badge-danger' : 'badge-warning'}`}>
                                  {m.status || 'PENDING'}
                                </span>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </>
            )}
          </div>
        )}


        {/* TAB 5: PROFILE */}
        {activeTab === 'profile' && (
          <div className="card" style={{ maxWidth: '720px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', borderBottom: '1px solid var(--border-main)', paddingBottom: '14px' }}>
              <div>
                <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#0f172a' }}>Citizen Profile & KYC Details</h3>
                <p style={{ fontSize: '12.5px', color: '#64748b', marginTop: '2px' }}>
                  Manage your verified direct benefit transfer (DBT) beneficiary credentials.
                </p>
              </div>
              {!isEditingProfile && (
                <button
                  type="button"
                  onClick={handleStartEditProfile}
                  className="btn btn-outline btn-sm"
                  style={{ gap: '6px' }}
                >
                  <Edit3 size={14} />
                  <span>Edit Profile</span>
                </button>
              )}
            </div>

            {isEditingProfile ? (
              <form onSubmit={handleSaveProfile} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label">Full Name <span className="req">*</span></label>
                    <input
                      type="text"
                      required
                      className="form-input"
                      value={profileForm.fullName}
                      onChange={(e) => setProfileForm({ ...profileForm, fullName: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Aadhaar (National ID)</label>
                    <input
                      type="text"
                      disabled
                      className="form-input"
                      value={beneficiary?.nationalIdNumber || '•••• •••• ••••'}
                      style={{ backgroundColor: '#f1f5f9', cursor: 'not-allowed' }}
                    />
                    <span style={{ fontSize: '11px', color: '#64748b' }}>Aadhaar cannot be modified once verified.</span>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label">Mobile Number <span className="req">*</span></label>
                    <input
                      type="tel"
                      required
                      placeholder="10-digit mobile number"
                      className="form-input"
                      value={profileForm.phoneNumber}
                      onChange={(e) => setProfileForm({ ...profileForm, phoneNumber: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Social Category <span className="req">*</span></label>
                    <select
                      className="form-select"
                      value={profileForm.category}
                      onChange={(e) => setProfileForm({ ...profileForm, category: e.target.value })}
                    >
                      <option value="GENERAL">General (Non-Reserved)</option>
                      <option value="SC">Scheduled Caste (SC)</option>
                      <option value="ST">Scheduled Tribe (ST)</option>
                      <option value="OBC">Other Backward Class (OBC)</option>
                      <option value="EWS">Economically Weaker Section (EWS)</option>
                    </select>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label">State / Region <span className="req">*</span></label>
                    <select
                      className="form-select"
                      value={profileForm.region}
                      onChange={(e) => setProfileForm({ ...profileForm, region: e.target.value })}
                    >
                      {INDIAN_STATES.map((st) => (
                        <option key={st} value={st}>{st}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Annual Household Income (₹) <span className="req">*</span></label>
                    <input
                      type="number"
                      required
                      min="0"
                      className="form-input"
                      value={profileForm.annualIncome}
                      onChange={(e) => setProfileForm({ ...profileForm, annualIncome: e.target.value })}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Permanent Residential Address <span className="req">*</span></label>
                  <textarea
                    rows={2}
                    required
                    className="form-textarea"
                    value={profileForm.address}
                    onChange={(e) => setProfileForm({ ...profileForm, address: e.target.value })}
                    placeholder="House number, Street, Village, Tehsil, District, PIN Code"
                  />
                </div>

                <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end', marginTop: '8px' }}>
                  <button
                    type="button"
                    onClick={() => setIsEditingProfile(false)}
                    className="btn btn-secondary"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={updateProfileMutation.isPending}
                    className="btn btn-primary"
                    style={{ gap: '6px' }}
                  >
                    <Save size={15} />
                    <span>{updateProfileMutation.isPending ? 'Saving...' : 'Save Profile Changes'}</span>
                  </button>
                </div>
              </form>
            ) : (
              <div>
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '16px',
                  marginBottom: '20px'
                }}>
                  <div style={{ padding: '14px', borderRadius: '8px', border: '1px solid var(--border-main)', backgroundColor: 'var(--bg-card)' }}>
                    <div style={{ fontSize: '11.5px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase' }}>Full Legal Name</div>
                    <div style={{ fontSize: '15px', fontWeight: 700, color: '#0f172a', marginTop: '4px' }}>
                      {beneficiary?.fullName || user?.name || 'Not specified'}
                    </div>
                  </div>

                  <div style={{ padding: '14px', borderRadius: '8px', border: '1px solid var(--border-main)', backgroundColor: 'var(--bg-card)' }}>
                    <div style={{ fontSize: '11.5px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase' }}>National ID (Aadhaar)</div>
                    <div style={{ fontSize: '15px', fontWeight: 700, color: '#0f172a', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <span>{beneficiary?.nationalIdNumber || '•••• •••• ••••'}</span>
                      <ShieldCheck size={16} color="#059669" />
                    </div>
                  </div>

                  <div style={{ padding: '14px', borderRadius: '8px', border: '1px solid var(--border-main)', backgroundColor: 'var(--bg-card)' }}>
                    <div style={{ fontSize: '11.5px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase' }}>Mobile Phone</div>
                    <div style={{ fontSize: '15px', fontWeight: 700, color: '#0f172a', marginTop: '4px' }}>
                      {beneficiary?.phoneNumber || 'Not provided'}
                    </div>
                  </div>

                  <div style={{ padding: '14px', borderRadius: '8px', border: '1px solid var(--border-main)', backgroundColor: 'var(--bg-card)' }}>
                    <div style={{ fontSize: '11.5px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase' }}>Social Category</div>
                    <div style={{ marginTop: '4px' }}>
                      <span className="badge badge-primary">{beneficiary?.category || 'GENERAL'}</span>
                    </div>
                  </div>

                  <div style={{ padding: '14px', borderRadius: '8px', border: '1px solid var(--border-main)', backgroundColor: 'var(--bg-card)' }}>
                    <div style={{ fontSize: '11.5px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase' }}>State / Region</div>
                    <div style={{ fontSize: '15px', fontWeight: 700, color: '#0f172a', marginTop: '4px' }}>
                      {beneficiary?.region || 'All India'}
                    </div>
                  </div>

                  <div style={{ padding: '14px', borderRadius: '8px', border: '1px solid var(--border-main)', backgroundColor: 'var(--bg-card)' }}>
                    <div style={{ fontSize: '11.5px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase' }}>Annual Income</div>
                    <div style={{ fontSize: '15px', fontWeight: 700, color: '#059669', marginTop: '4px' }}>
                      {beneficiary?.annualIncome ? formatINRFull(beneficiary.annualIncome) : 'Not specified'}
                    </div>
                  </div>
                </div>

                <div style={{ padding: '14px', borderRadius: '8px', border: '1px solid var(--border-main)', backgroundColor: 'var(--bg-card)' }}>
                  <div style={{ fontSize: '11.5px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase', marginBottom: '4px' }}>Residential Address</div>
                  <p style={{ fontSize: '13.5px', color: '#334155', lineHeight: 1.5, margin: 0 }}>
                    {beneficiary?.address || 'No physical address recorded on file.'}
                  </p>
                </div>
              </div>
            )}
          </div>
        )}
      </main>

      {/* Apply Modal */}
      <Modal
        isOpen={applyModalOpen}
        onClose={() => setApplyModalOpen(false)}
        title={`Apply for ${selectedScheme?.name || selectedScheme?.schemeName || 'Government Scheme'}`}
        subtitle="Review scheme terms and initiate application"
      >
        <form onSubmit={handleApplySubmit}>
          <div className="form-group">
            <label className="form-label">Individual Grant Sanction Amount (₹)</label>
            <div
              style={{
                padding: '12px 14px',
                backgroundColor: '#f8fafc',
                border: '1px solid var(--border-main)',
                borderRadius: '6px',
                fontSize: '18px',
                fontWeight: 800,
                color: '#059669',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <span>{formatINRFull(fixedGrantAmount)}</span>
              <span className="badge badge-success">Individual Policy Slab</span>
            </div>
            <p style={{ fontSize: '11.5px', color: '#64748b', marginTop: '5px' }}>
              The individual grant amount you will receive is fixed by government scheme policy for your registered category ({beneficiary?.category || 'GENERAL'}).
            </p>
          </div>

          <div className="form-group">
            <label className="form-label">Mandatory Documents Required</label>
            <div style={{ fontSize: '13px', color: '#334155', fontWeight: 600 }}>
              {selectedScheme?.requiredDocuments || 'Aadhaar, Bank Passbook'}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Project / Purpose Remarks (Optional)</label>
            <textarea
              rows={3}
              placeholder="Describe how the grant or subsidy will be utilized..."
              className="form-textarea"
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          </div>

          <button
            type="submit"
            disabled={submitAppMutation.isPending}
            className="btn btn-primary btn-lg"
            style={{ width: '100%', justifyContent: 'center', marginTop: '10px' }}
          >
            {submitAppMutation.isPending ? 'Creating Draft...' : 'Confirm & Create Draft Application →'}
          </button>
        </form>
      </Modal>

      {/* Document Upload Modal — STRICTLY SCHEME CONFIGURED, NO DROPDOWN MENU */}
      <Modal
        isOpen={uploadModalOpen}
        onClose={() => setUploadModalOpen(false)}
        title={`Mandatory Documents for App #${selectedDocApp?.id}`}
        subtitle={`Scheme: ${
          schemes.find(s => s.id === (selectedDocApp?.scheme?.id || selectedDocApp?.schemeId))?.name ||
          selectedDocApp?.scheme?.schemeName ||
          selectedDocApp?.scheme?.name ||
          'Government Grant Scheme'
        }`}
      >
        {(() => {
          const docAppScheme = schemes.find(s => s.id === (selectedDocApp?.scheme?.id || selectedDocApp?.schemeId)) || selectedDocApp?.scheme;
          const reqDocsString = docAppScheme?.requiredDocuments || selectedDocApp?.scheme?.requiredDocuments || '';
          const requiredDocList = reqDocsString
            ? reqDocsString.split(',').map(d => d.trim()).filter(Boolean)
            : ['Aadhaar Card', 'Land Record', 'Bank Passbook'];

          const uploadedTypesMap = {};
          appDocuments.forEach((d) => {
            if (d.documentType) {
              uploadedTypesMap[d.documentType.trim().toLowerCase()] = d;
            }
          });

          const totalRequired = requiredDocList.length;
          const uploadedCount = requiredDocList.filter(req => !!uploadedTypesMap[req.toLowerCase()]).length;
          const allMandatoryUploaded = totalRequired > 0 && uploadedCount === totalRequired;

          return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {/* Requirement Status Summary Banner */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '12px 16px',
                backgroundColor: allMandatoryUploaded ? '#ecfdf5' : '#f8fafc',
                border: `1px solid ${allMandatoryUploaded ? '#a7f3d0' : '#e2e8f0'}`,
                borderRadius: '8px',
                flexWrap: 'wrap',
                gap: '10px'
              }}>
                <div>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: allMandatoryUploaded ? '#065f46' : '#1e293b' }}>
                    {allMandatoryUploaded
                      ? '✓ All mandatory scheme documents uploaded!'
                      : `Uploaded ${uploadedCount} of ${totalRequired} mandatory scheme documents`}
                  </div>
                  <div style={{ fontSize: '12px', color: allMandatoryUploaded ? '#047857' : '#64748b', marginTop: '2px' }}>
                    {allMandatoryUploaded
                      ? 'All verification documents configured for this scheme have been uploaded and linked.'
                      : 'Upload the documents configured specifically for this scheme below.'}
                  </div>
                </div>
                {selectedDocApp?.status === 'DRAFT' && allMandatoryUploaded && (
                  <button
                    onClick={() => {
                      submitDraftMutation.mutate(selectedDocApp.id);
                      setUploadModalOpen(false);
                    }}
                    disabled={submitDraftMutation.isPending}
                    className="btn btn-success btn-sm"
                  >
                    <Send size={13} />
                    <span>Submit Draft Application</span>
                  </button>
                )}
              </div>

              {/* Scheme Required Documents List (NO DROPDOWN MENU) */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {requiredDocList.map((docName, idx) => {
                  const uploadedDoc = uploadedTypesMap[docName.toLowerCase()];
                  const isUploaded = !!uploadedDoc;
                  const isUploading = uploadingDocType === docName;
                  const currentFile = selectedFiles[docName];

                  return (
                    <div
                      key={docName + idx}
                      style={{
                        border: `1px solid ${isUploaded ? '#bbf7d0' : '#e2e8f0'}`,
                        borderRadius: '8px',
                        padding: '14px',
                        backgroundColor: isUploaded ? '#f0fdf4' : '#ffffff',
                        boxShadow: '0 1px 3px rgba(0,0,0,0.03)'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px', flexWrap: 'wrap', gap: '8px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          {isUploaded ? (
                            <CheckCircle2 size={18} color="#16a34a" />
                          ) : (
                            <AlertCircle size={18} color="#d97706" />
                          )}
                          <div>
                            <span style={{ fontWeight: 700, fontSize: '14px', color: '#1e293b' }}>
                              {docName}
                            </span>
                            <span className="badge badge-secondary" style={{ marginLeft: '8px', fontSize: '11px' }}>
                              Required by Scheme
                            </span>
                          </div>
                        </div>

                        {isUploaded && (
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span className={`badge ${
                              uploadedDoc.verificationStatus === 'VERIFIED'
                                ? 'badge-success'
                                : uploadedDoc.verificationStatus === 'REJECTED'
                                ? 'badge-danger'
                                : 'badge-warning'
                            }`}>
                              {uploadedDoc.verificationStatus || 'PENDING VERIFICATION'}
                            </span>
                            <button
                              type="button"
                              onClick={() => setPreviewDoc(uploadedDoc)}
                              className="btn btn-outline btn-sm"
                              style={{ padding: '4px 8px', fontSize: '11.5px', display: 'flex', alignItems: 'center', gap: '4px' }}
                              title="View uploaded file online"
                            >
                              <Eye size={12} />
                              <span>View Online</span>
                            </button>
                          </div>
                        )}
                      </div>

                      {uploadedDoc?.verificationRemarks && (
                        <div style={{
                          fontSize: '12px',
                          color: uploadedDoc.verificationStatus === 'REJECTED' ? '#b91c1c' : '#475569',
                          marginBottom: '8px',
                          padding: '6px 10px',
                          backgroundColor: uploadedDoc.verificationStatus === 'REJECTED' ? '#fee2e2' : '#f8fafc',
                          borderRadius: '4px'
                        }}>
                          <strong>Officer Remarks:</strong> {uploadedDoc.verificationRemarks}
                        </div>
                      )}

                      {/* Direct File Input & Upload Action — No dropdown menu */}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginTop: '6px', flexWrap: 'wrap' }}>
                        <input
                          type="file"
                          accept=".pdf,.jpg,.jpeg,.png"
                          style={{ fontSize: '12.5px', flex: 1, minWidth: '200px' }}
                          onChange={(e) => {
                            const file = e.target.files?.[0];
                            if (file) {
                              setSelectedFiles((prev) => ({ ...prev, [docName]: file }));
                            }
                          }}
                        />
                        <button
                          type="button"
                          disabled={!currentFile || isUploading || uploadDocMutation.isPending}
                          onClick={() => {
                            if (!currentFile) return;
                            setUploadingDocType(docName);
                            uploadDocMutation.mutate({
                              appId: selectedDocApp.id,
                              file: currentFile,
                              type: docName,
                            });
                          }}
                          className={`btn btn-sm ${isUploaded ? 'btn-secondary' : 'btn-primary'}`}
                        >
                          <Upload size={13} />
                          <span>
                            {isUploading
                              ? 'Uploading...'
                              : isUploaded
                              ? `Replace ${docName}`
                              : `Upload ${docName}`}
                          </span>
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Section: Stage Utilization Proof (DBT Milestones) */}
              {(selectedDocApp?.status === 'SANCTIONED' || selectedDocApp?.status === 'DISBURSED' || selectedDocApp?.status === 'FINANCE_APPROVED') && (
                <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: '14px', marginTop: '4px' }}>
                  <h4 style={{ fontSize: '13.5px', fontWeight: 700, color: '#334155', marginBottom: '4px' }}>
                    Stage Utilization Proof (DBT Tranche Milestones)
                  </h4>
                  <p style={{ fontSize: '12px', color: '#64748b', marginBottom: '10px' }}>
                    If a subsidy tranche was released to your bank account, upload utilization evidence or geo-tagged site photos here.
                  </p>
                  <div style={{ display: 'grid', gridTemplateColumns: '120px 1fr auto', gap: '10px', alignItems: 'center' }}>
                    <div>
                      <input
                        type="number"
                        min="1"
                        placeholder="Stage #"
                        className="form-input"
                        style={{ fontSize: '12.5px', padding: '6px 10px' }}
                        value={uploadStageId}
                        onChange={(e) => setUploadStageId(e.target.value)}
                      />
                    </div>
                    <div>
                      <input
                        type="file"
                        accept=".pdf,.jpg,.jpeg,.png"
                        style={{ fontSize: '12.5px' }}
                        onChange={(e) => setStageProofFile(e.target.files?.[0])}
                      />
                    </div>
                    <div>
                      <button
                        type="button"
                        disabled={!stageProofFile || !uploadStageId || uploadDocMutation.isPending}
                        onClick={() => {
                          if (!stageProofFile || !uploadStageId) return;
                          setUploadingDocType('STAGE_UTILIZATION_PROOF');
                          uploadDocMutation.mutate({
                            appId: selectedDocApp.id,
                            file: stageProofFile,
                            type: 'STAGE_UTILIZATION_PROOF',
                            stageId: uploadStageId,
                          });
                        }}
                        className="btn btn-secondary btn-sm"
                      >
                        <Upload size={13} />
                        <span>Upload Stage Proof</span>
                      </button>
                    </div>
                  </div>
                </div>
              )}

              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '12px' }}>
                <button
                  type="button"
                  onClick={() => setUploadModalOpen(false)}
                  className="btn btn-secondary btn-sm"
                >
                  Done / Close
                </button>
              </div>
            </div>
          );
        })()}
      </Modal>


      {/* View Documents Modal (GET /api/v1/applications/{appId}/documents) */}
      <Modal
        isOpen={docsModalOpen}
        onClose={() => setDocsModalOpen(false)}
        title={`Attached Documents (App #${selectedDocsApp?.id})`}
        subtitle={`Applicant: ${selectedDocsApp?.beneficiary?.fullName || 'Citizen'} • Scheme: ${selectedDocsApp?.scheme?.schemeName}`}
      >
        {docsLoading ? (
          <p>Loading documents...</p>
        ) : appDocuments.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '30px 12px', color: '#64748b' }}>
            <FileText size={36} style={{ margin: '0 auto 10px', opacity: 0.5 }} />
            <p style={{ fontWeight: 600 }}>No documents attached yet</p>
            <p style={{ fontSize: '12.5px', marginTop: '4px' }}>Click "Upload" to attach required KYC and land records.</p>
          </div>
        ) : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Doc ID</th>
                  <th>Document Type</th>
                  <th>Verification Status</th>
                  <th>Officer Remarks</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {appDocuments.map((doc) => (
                  <tr key={doc.id}>
                    <td><code>#{doc.id}</code></td>
                    <td><strong>{doc.documentType?.replace('_', ' ')}</strong></td>
                    <td>
                      <span className={`badge ${doc.verificationStatus === 'VERIFIED' ? 'badge-success' : doc.verificationStatus === 'REJECTED' ? 'badge-danger' : 'badge-warning'}`}>
                        {doc.verificationStatus || 'PENDING'}
                      </span>
                    </td>
                    <td>{doc.verificationRemarks || '—'}</td>
                    <td>
                      <button
                        type="button"
                        onClick={() => setPreviewDoc(doc)}
                        className="btn btn-secondary btn-sm"
                        style={{ display: 'flex', alignItems: 'center', gap: '4px' }}
                        title="View Document Online"
                      >
                        <Eye size={13} />
                        <span>View Online</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Modal>

      {/* View Application Details Modal (GET /api/v1/applications/{id}) */}
      <Modal
        isOpen={appDetailModalOpen}
        onClose={() => setAppDetailModalOpen(false)}
        title={`Application Dossier #${singleAppDetail?.id || selectedAppId}`}
        subtitle={`Scheme: ${singleAppDetail?.scheme?.schemeName || singleAppDetail?.scheme?.name}`}
      >
        {singleAppLoading ? (
          <p>Loading dossier...</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', backgroundColor: '#f8fafc', padding: '16px', borderRadius: '8px' }}>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Beneficiary Name</div>
                <div style={{ fontSize: '15px', fontWeight: 700 }}>{singleAppDetail?.beneficiary?.fullName}</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>National ID (Aadhaar)</div>
                <div style={{ fontSize: '14px', fontWeight: 600 }}>•••• •••• {singleAppDetail?.beneficiary?.nationalIdNumber?.slice(-4) || '1234'}</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Social Category</div>
                <div><span className="badge badge-primary">{singleAppDetail?.beneficiary?.category}</span></div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Annual Income</div>
                <div style={{ fontSize: '14px', fontWeight: 700 }}>{formatINRFull(singleAppDetail?.beneficiary?.annualIncome || 0)}</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Requested Subsidy Amount</div>
                <div style={{ fontSize: '16px', fontWeight: 800, color: '#059669' }}>{formatINRFull(singleAppDetail?.requestedAmount)}</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Application Status</div>
                <div><StatusBadge status={singleAppDetail?.status} /></div>
              </div>
            </div>

            {singleAppDetail?.remarks && (
              <div style={{ padding: '12px', border: '1px solid #e2e8f0', borderRadius: '6px' }}>
                <div style={{ fontSize: '12px', color: '#64748b', fontWeight: 600 }}>Application Remarks:</div>
                <div style={{ fontSize: '13px', marginTop: '4px' }}>{singleAppDetail.remarks}</div>
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* Online Document Viewer Modal */}
      <DocumentViewerModal
        isOpen={!!previewDoc}
        onClose={() => setPreviewDoc(null)}
        document={previewDoc}
        applicationId={selectedDocApp?.id || selectedDocsApp?.id}
      />
    </div>
  );
}

