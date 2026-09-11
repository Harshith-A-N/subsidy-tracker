import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient, downloadFile } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { formatINR, formatINRFull, formatDate, formatDateTime } from '../utils/formatters';
import { BENEFICIARY_CATEGORIES, INDIAN_STATES } from '../utils/constants';
import Sidebar from '../components/Sidebar';
import MetricCard from '../components/MetricCard';
import StatusBadge from '../components/StatusBadge';
import Modal from '../components/Modal';

// Charts
import PipelineStageChart from '../components/charts/PipelineStageChart';
import BudgetUtilizationDonut from '../components/charts/BudgetUtilizationDonut';
import RegionalApplicationsChart from '../components/charts/RegionalApplicationsChart';
import RegionalUtilizationChart from '../components/charts/RegionalUtilizationChart';
import DisbursementTrendsChart from '../components/charts/DisbursementTrendsChart';
import SchemeDistributionChart from '../components/charts/SchemeDistributionChart';
import CategoryInclusivityChart from '../components/charts/CategoryInclusivityChart';

import {
  LayoutDashboard,
  BarChart3,
  UserCheck,
  FolderOpen,
  PlusCircle,
  Users,
  FileText,
  CheckCircle2,
  XCircle,
  Clock,
  IndianRupee,
  Building,
  ShieldCheck,
  Search,
  Download,
  FileSpreadsheet,
  FileDown,
  Layers,
  MapPin,
  Eye,
  Edit2,
  Trash2,
  RefreshCw,
  TrendingUp,
  AlertOctagon,
  History,
  Menu
} from 'lucide-react';

export default function AdminPortal() {
  const [activeTab, setActiveTab] = useState('overview');
  const [newSchemeModalOpen, setNewSchemeModalOpen] = useState(false);
  const [editSchemeModalOpen, setEditSchemeModalOpen] = useState(false);
  const [selectedScheme, setSelectedScheme] = useState(null);
  const [slabsModalOpen, setSlabsModalOpen] = useState(false);
  const [regionalBudgetsModalOpen, setRegionalBudgetsModalOpen] = useState(false);
  const [disbursementPlanModalOpen, setDisbursementPlanModalOpen] = useState(false);
  
  const [appSearch, setAppSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [selectedAppId, setSelectedAppId] = useState(null);
  const [appDetailModalOpen, setAppDetailModalOpen] = useState(false);

  const [selectedBeneficiaryId, setSelectedBeneficiaryId] = useState(null);
  const [beneficiaryModalOpen, setBeneficiaryModalOpen] = useState(false);
  const [beneficiarySearch, setBeneficiarySearch] = useState('');

  // Scheme Form State (used for Edit Scheme)
  const [schemeForm, setSchemeForm] = useState({
    name: '',
    description: '',
    minIncome: '',
    maxIncome: '',
    allowedCategories: 'GENERAL,SC,ST,OBC,EWS',
    isActive: true,
    requiredDocuments: 'Aadhaar Card, Land Record, Bank Passbook',
  });

  // Unified Scheme Creation Package Form State (Manually configured by Admin)
  const defaultSchemePackage = {
    name: '',
    description: '',
    minIncome: '0',
    maxIncome: '300000',
    allowedCategories: 'GENERAL,SC,ST,OBC,EWS',
    requiredDocuments: 'Aadhaar Card, Land Record, Bank Passbook',
    slabs: [
      { category: 'GENERAL', grantAmount: '50000' },
      { category: 'SC', grantAmount: '65000' },
      { category: 'ST', grantAmount: '65000' },
      { category: 'OBC', grantAmount: '55000' },
      { category: 'EWS', grantAmount: '60000' },
    ],
    regionalBudgets: [
      { regionName: 'Maharashtra', allocatedBudget: '5000000' },
      { regionName: 'Karnataka', allocatedBudget: '3500000' },
      { regionName: 'Gujarat', allocatedBudget: '3000000' },
    ],
    stages: [
      { stageName: 'Initial Advance', percentageOfGrant: 30, triggerMilestone: 'DOCUMENT_VERIFIED', dueDateOffsetDays: 7 },
      { stageName: 'Work In Progress / Ground Inspection', percentageOfGrant: 40, triggerMilestone: 'FIELD_INSPECTION_PASSED', dueDateOffsetDays: 30 },
      { stageName: 'Final Sanction / Project Completion', percentageOfGrant: 30, triggerMilestone: 'PROJECT_COMPLETED', dueDateOffsetDays: 60 },
    ],
  };

  const [schemePackage, setSchemePackage] = useState(defaultSchemePackage);
  const [activePackageTab, setActivePackageTab] = useState('core');
  const [isPublishingPackage, setIsPublishingPackage] = useState(false);
  const [newSlabInput, setNewSlabInput] = useState({ category: 'GENERAL', grantAmount: '' });
  const [newRegionInput, setNewRegionInput] = useState({ regionName: 'Rajasthan', allocatedBudget: '' });

  // Slab Form State
  const [slabForm, setSlabForm] = useState({
    category: 'GENERAL',
    grantAmount: '',
  });

  // Regional Budget Form State
  const [regionalBudgetForm, setRegionalBudgetForm] = useState({
    regionName: 'Maharashtra',
    allocatedBudget: '',
  });

  // Disbursement Plan Form State
  const [planForm, setPlanForm] = useState({
    stages: [
      { stageName: 'Initial Advance', sequenceNumber: 1, percentageOfGrant: 30, triggerMilestone: 'DOCUMENT_VERIFIED', dueDateOffsetDays: 7 },
      { stageName: 'Work In Progress', sequenceNumber: 2, percentageOfGrant: 40, triggerMilestone: 'FIELD_INSPECTION_PASSED', dueDateOffsetDays: 30 },
      { stageName: 'Final Sanction', sequenceNumber: 3, percentageOfGrant: 30, triggerMilestone: 'PROJECT_COMPLETED', dueDateOffsetDays: 60 },
    ]
  });

  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();

  // Officer Registration Requests Query
  const { data: officerRequests = [] } = useQuery({
    queryKey: ['admin-officer-requests'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/admin/officer-registration-requests/all');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Schemes Query
  const { data: schemes = [] } = useQuery({
    queryKey: ['admin-schemes'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/schemes');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Master Applications Query (with status filter support)
  const { data: allApplications = [] } = useQuery({
    queryKey: ['admin-all-applications', statusFilter],
    queryFn: async () => {
      const url = statusFilter
        ? `/api/v1/applications/status/${statusFilter}?size=100`
        : '/api/v1/applications?size=100';
      const res = await apiClient.get(url);
      if (!res.success) return [];
      if (Array.isArray(res.data)) return res.data;
      if (res.data && Array.isArray(res.data.content)) return res.data.content;
      return [];
    },
    staleTime: 2 * 60 * 1000,
  });

  // Single Application Detail Query
  const { data: applicationDetail, isLoading: appDetailLoading } = useQuery({
    queryKey: ['admin-application-detail', selectedAppId],
    queryFn: async () => {
      if (!selectedAppId) return null;
      const res = await apiClient.get(`/api/v1/applications/${selectedAppId}`);
      return res.success ? res.data : null;
    },
    enabled: !!selectedAppId,
  });

  // Users Directory Query
  const { data: allUsers = [] } = useQuery({
    queryKey: ['admin-all-users'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/users');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Beneficiaries Query (GET /api/v1/beneficiaries)
  const { data: allBeneficiaries = [] } = useQuery({
    queryKey: ['admin-all-beneficiaries'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/beneficiaries');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Single Beneficiary Detail Query
  const { data: beneficiaryDetail, isLoading: beneficiaryLoading } = useQuery({
    queryKey: ['admin-beneficiary-detail', selectedBeneficiaryId],
    queryFn: async () => {
      if (!selectedBeneficiaryId) return null;
      const res = await apiClient.get(`/api/v1/beneficiaries/${selectedBeneficiaryId}`);
      return res.success ? res.data : null;
    },
    enabled: !!selectedBeneficiaryId,
  });

  // Regional Analytics Query
  const { data: regionalData = [] } = useQuery({
    queryKey: ['admin-regional-analytics'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/analytics/fund-utilization/regions');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Analytics Overview Query
  const { data: analyticsOverview } = useQuery({
    queryKey: ['admin-analytics-overview'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/analytics/overview');
      return res.success && res.data ? res.data : null;
    },
    staleTime: 5 * 60 * 1000,
  });

  // Scheme Utilization Query
  const { data: schemeUtilization = [] } = useQuery({
    queryKey: ['admin-scheme-utilization'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/analytics/fund-utilization/schemes');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Disbursement Trends Query
  const { data: disbursementTrends = [] } = useQuery({
    queryKey: ['admin-disbursement-trends'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/analytics/disbursement-trends');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  // Advanced Analytics Queries
  const { data: turnaroundData } = useQuery({
    queryKey: ['admin-approval-turnaround'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/analytics/approval-turnaround');
      return res.success ? res.data : null;
    },
    enabled: activeTab === 'advanced-analytics',
  });

  const { data: budgetWarnings = [] } = useQuery({
    queryKey: ['admin-budget-warnings'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/analytics/budget-exhaustion-warnings');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: activeTab === 'advanced-analytics',
  });

  const { data: categoryDistributions = [] } = useQuery({
    queryKey: ['admin-category-distribution'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/analytics/beneficiary-category-distribution');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: activeTab === 'advanced-analytics',
  });

  // Audit Logs State & Query
  const [auditEntityFilter, setAuditEntityFilter] = useState('');
  const [auditActionFilter, setAuditActionFilter] = useState('');
  const [auditSearchTerm, setAuditSearchTerm] = useState('');

  const { data: auditLogs = [], isLoading: auditLogsLoading, refetch: refetchAuditLogs } = useQuery({
    queryKey: ['admin-audit-logs', auditEntityFilter, auditActionFilter],
    queryFn: async () => {
      const params = {};
      if (auditEntityFilter) params.entityName = auditEntityFilter;
      if (auditActionFilter) params.action = auditActionFilter;
      const res = await apiClient.get('/api/v1/audit-logs', { params });
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: activeTab === 'audit-logs',
  });

  // Query audit trail for the selected application dossier
  const { data: appAuditLogs = [], isLoading: appAuditLoading } = useQuery({
    queryKey: ['app-audit-logs', selectedAppId],
    queryFn: async () => {
      if (!selectedAppId) return [];
      const res = await apiClient.get(`/api/v1/audit-logs/application/${selectedAppId}`);
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!selectedAppId && appDetailModalOpen,
  });

  // Scheme Slabs Query for Selected Scheme
  const { data: schemeSlabs = [], refetch: refetchSlabs } = useQuery({
    queryKey: ['admin-scheme-slabs', selectedScheme?.id],
    queryFn: async () => {
      if (!selectedScheme?.id) return [];
      const res = await apiClient.get(`/api/v1/schemes/${selectedScheme.id}/slabs`);
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!selectedScheme?.id && slabsModalOpen,
  });

  // Regional Budgets Query for Selected Scheme
  const { data: schemeRegionalBudgets = [], refetch: refetchRegionalBudgets } = useQuery({
    queryKey: ['admin-scheme-regional-budgets', selectedScheme?.id],
    queryFn: async () => {
      if (!selectedScheme?.id) return [];
      const res = await apiClient.get(`/api/v1/schemes/${selectedScheme.id}/regional-budgets`);
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!selectedScheme?.id && regionalBudgetsModalOpen,
  });

  // Disbursement Plan Query for Selected Scheme
  const { data: schemeDisbursementPlan, refetch: refetchPlan } = useQuery({
    queryKey: ['admin-scheme-disbursement-plan', selectedScheme?.id],
    queryFn: async () => {
      if (!selectedScheme?.id) return null;
      const res = await apiClient.get(`/api/disbursement/plans/scheme/${selectedScheme.id}`);
      return res.success ? res.data : null;
    },
    enabled: !!selectedScheme?.id && disbursementPlanModalOpen,
  });

  // Officer Request Decision Mutation
  const officerRequestMutation = useMutation({
    mutationFn: async ({ id, action }) => {
      return apiClient.post(`/api/v1/admin/officer-registration-requests/${id}/${action}`);
    },
    onSuccess: (res, vars) => {
      if (res.success) {
        toast.success(`Officer request #${vars.id} has been ${vars.action}ed!`);
        queryClient.invalidateQueries(['admin-officer-requests']);
        queryClient.invalidateQueries(['admin-all-users']);
      } else {
        toast.error(res.error || 'Action failed');
      }
    },
  });

  // Create Scheme Mutation
  const createSchemeMutation = useMutation({
    mutationFn: async (payload) => {
      return apiClient.post('/api/v1/schemes', payload);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success('New scheme published successfully!');
        queryClient.invalidateQueries(['admin-schemes']);
        setNewSchemeModalOpen(false);
      } else {
        toast.error(res.error || 'Failed to create scheme');
      }
    },
  });

  // Update Scheme Mutation
  const updateSchemeMutation = useMutation({
    mutationFn: async ({ id, payload }) => {
      return apiClient.put(`/api/v1/schemes/${id}`, payload);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success('Scheme updated successfully!');
        queryClient.invalidateQueries(['admin-schemes']);
        queryClient.invalidateQueries(['public-schemes']);
        setEditSchemeModalOpen(false);
      } else {
        toast.error(res.error || 'Failed to update scheme');
      }
    },
  });

  // Toggle Scheme Active Status Mutation
  const toggleSchemeActiveMutation = useMutation({
    mutationFn: async (scheme) => {
      const currentActive = scheme.isActive !== undefined ? Boolean(scheme.isActive) : (scheme.active !== undefined ? Boolean(scheme.active) : true);
      const newActive = !currentActive;
      return apiClient.put(`/api/v1/schemes/${scheme.id}`, {
        name: scheme.name || scheme.schemeName,
        description: scheme.description,
        minIncome: scheme.minIncome !== undefined ? parseFloat(scheme.minIncome) : 0,
        maxIncome: scheme.maxIncome !== undefined ? parseFloat(scheme.maxIncome) : 300000,
        allowedCategories: scheme.allowedCategories || 'GENERAL,SC,ST,OBC,EWS',
        isActive: newActive,
        active: newActive,
        requiredDocuments: scheme.requiredDocuments || 'Aadhaar Card, Bank Passbook',
      });
    },
    onSuccess: (res, scheme) => {
      if (res.success) {
        const currentActive = scheme.isActive !== undefined ? Boolean(scheme.isActive) : (scheme.active !== undefined ? Boolean(scheme.active) : true);
        toast.success(`Scheme "${scheme.name || scheme.schemeName}" is now ${!currentActive ? 'ACTIVE' : 'INACTIVE'}!`);
        queryClient.invalidateQueries(['admin-schemes']);
        queryClient.invalidateQueries(['public-schemes']);
      } else {
        toast.error(res.error || 'Failed to toggle scheme status');
      }
    },
  });

  // Add Slab Mutation
  const addSlabMutation = useMutation({
    mutationFn: async ({ schemeId, payload }) => {
      return apiClient.post(`/api/v1/schemes/${schemeId}/slabs`, payload);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success('Grant slab added successfully!');
        refetchSlabs();
        setSlabForm({ category: 'GENERAL', grantAmount: '' });
      } else {
        toast.error(res.error || 'Failed to add slab');
      }
    },
  });

  // Add Regional Budget Mutation
  const addRegionalBudgetMutation = useMutation({
    mutationFn: async ({ schemeId, payload }) => {
      return apiClient.post(`/api/v1/schemes/${schemeId}/regional-budgets`, payload);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success('Regional budget allocated successfully!');
        refetchRegionalBudgets();
        setRegionalBudgetForm({ regionName: 'Maharashtra', allocatedBudget: '' });
      } else {
        toast.error(res.error || 'Failed to add regional budget');
      }
    },
  });

  // Create Disbursement Plan Mutation
  const createPlanMutation = useMutation({
    mutationFn: async (payload) => {
      return apiClient.post('/api/disbursement/plans', payload);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success('Disbursement plan configured successfully!');
        refetchPlan();
      } else {
        toast.error(res.error || 'Failed to configure plan');
      }
    },
  });

  // Delete Disbursement Plan Mutation
  const deletePlanMutation = useMutation({
    mutationFn: async (planId) => {
      return apiClient.delete(`/api/disbursement/plans/${planId}`);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success('Disbursement plan deleted!');
        refetchPlan();
      } else {
        toast.error(res.error || 'Failed to delete plan');
      }
    },
  });

  // Recalculate Eligibility Mutation
  const recalculateEligibilityMutation = useMutation({
    mutationFn: async (applicationId) => {
      return apiClient.post(`/api/v1/applications/${applicationId}/calculate-eligibility`);
    },
    onSuccess: (res) => {
      if (res.success) {
        toast.success(`Eligibility recalculated: Status is now ${res.data?.status}`);
        queryClient.invalidateQueries(['admin-application-detail', selectedAppId]);
        queryClient.invalidateQueries(['admin-all-applications']);
      } else {
        toast.error(res.error || 'Recalculation failed');
      }
    },
  });


  // Report Download Handler
  const handleDownloadReport = async (url, filename) => {
    toast.info(`Generating ${filename}...`);
    const res = await downloadFile(url, filename);
    if (res.success) {
      toast.success(`Downloaded ${filename}!`);
    } else {
      toast.error(res.error || 'Download failed');
    }
  };

  // Unified Scheme Package Creation Handler (Core + Slabs + Regional Budgets + Dynamic Plan)
  const handleCreateSchemePackage = async (e) => {
    if (e) e.preventDefault();

    if (!schemePackage.name?.trim() || !schemePackage.description?.trim()) {
      toast.error('Scheme official name and description are required.');
      setActivePackageTab('core');
      return;
    }

    if (!schemePackage.requiredDocuments?.trim()) {
      toast.error('At least one required document must be configured for the scheme.');
      setActivePackageTab('core');
      return;
    }

    if (!schemePackage.stages || schemePackage.stages.length === 0) {
      toast.error('At least one disbursement stage is required in the plan.');
      setActivePackageTab('plan');
      return;
    }

    const totalPercentage = schemePackage.stages.reduce(
      (sum, s) => sum + (Number(s.percentageOfGrant) || 0),
      0
    );

    if (Math.round(totalPercentage) !== 100) {
      toast.error(
        `Disbursement stage percentages must total exactly 100%. Currently: ${totalPercentage}%`
      );
      setActivePackageTab('plan');
      return;
    }

    setIsPublishingPackage(true);
    try {
      // 1. Create Core Scheme
      const schemeRes = await apiClient.post('/api/v1/schemes', {
        name: schemePackage.name.trim(),
        description: schemePackage.description.trim(),
        minIncome: parseFloat(schemePackage.minIncome) || 0,
        maxIncome: parseFloat(schemePackage.maxIncome) || 300000,
        allowedCategories: schemePackage.allowedCategories,
        isActive: true,
        requiredDocuments: schemePackage.requiredDocuments.trim(),
      });

      if (!schemeRes.success || !schemeRes.data?.id) {
        throw new Error(schemeRes.error || 'Failed to publish scheme core details');
      }

      const newSchemeId = schemeRes.data.id;

      // 2. Create Category Slabs
      for (const slab of schemePackage.slabs) {
        if (slab.grantAmount && Number(slab.grantAmount) > 0) {
          await apiClient.post(`/api/v1/schemes/${newSchemeId}/slabs`, {
            category: slab.category,
            grantAmount: parseFloat(slab.grantAmount),
          });
        }
      }

      // 3. Create Regional Budgets
      for (const b of schemePackage.regionalBudgets) {
        if (b.allocatedBudget && Number(b.allocatedBudget) > 0) {
          await apiClient.post(`/api/v1/schemes/${newSchemeId}/regional-budgets`, {
            regionName: b.regionName,
            allocatedBudget: parseFloat(b.allocatedBudget),
          });
        }
      }

      // 4. Create Dynamic Disbursement Plan decided by Admin
      const stagesPayload = schemePackage.stages.map((st, idx) => ({
        stageName: st.stageName || `Stage ${idx + 1}`,
        sequenceNumber: idx + 1,
        percentageOfGrant: parseFloat(st.percentageOfGrant),
        triggerMilestone: st.triggerMilestone || 'DOCUMENT_VERIFIED',
        dueDateOffsetDays: parseInt(st.dueDateOffsetDays) >= 0 ? parseInt(st.dueDateOffsetDays) : 7,
      }));

      await apiClient.post('/api/disbursement/plans', {
        schemeId: newSchemeId,
        stages: stagesPayload,
      });

      toast.success(
        `Scheme "${schemePackage.name}" created successfully with ${schemePackage.slabs.length} grant slabs, ${schemePackage.regionalBudgets.length} regional allocations, and ${stagesPayload.length}-stage disbursement plan!`
      );
      queryClient.invalidateQueries(['admin-schemes']);
      setNewSchemeModalOpen(false);
      setSchemePackage(defaultSchemePackage);
      setActivePackageTab('core');
    } catch (err) {
      toast.error(err.message || 'Failed to complete scheme configuration');
    } finally {
      setIsPublishingPackage(false);
    }
  };

  const handleCreateScheme = (e) => {
    handleCreateSchemePackage(e);
  };

  const handleUpdateScheme = (e) => {
    e.preventDefault();
    if (!selectedScheme) return;
    updateSchemeMutation.mutate({
      id: selectedScheme.id,
      payload: {
        name: schemeForm.name,
        description: schemeForm.description,
        minIncome: parseFloat(schemeForm.minIncome) || 0,
        maxIncome: parseFloat(schemeForm.maxIncome) || 300000,
        allowedCategories: schemeForm.allowedCategories,
        isActive: Boolean(schemeForm.isActive),
        active: Boolean(schemeForm.isActive),
        requiredDocuments: schemeForm.requiredDocuments,
      },
    });
  };

  const handleAddSlab = (e) => {
    e.preventDefault();
    if (!selectedScheme) return;
    addSlabMutation.mutate({
      schemeId: selectedScheme.id,
      payload: {
        category: slabForm.category,
        grantAmount: parseFloat(slabForm.grantAmount) || 50000,
      },
    });
  };

  const handleAddRegionalBudget = (e) => {
    e.preventDefault();
    if (!selectedScheme) return;
    addRegionalBudgetMutation.mutate({
      schemeId: selectedScheme.id,
      payload: {
        regionName: regionalBudgetForm.regionName,
        allocatedBudget: parseFloat(regionalBudgetForm.allocatedBudget) || 1000000,
      },
    });
  };

  const handleSaveDisbursementPlan = (e) => {
    e.preventDefault();
    if (!selectedScheme) return;
    createPlanMutation.mutate({
      schemeId: selectedScheme.id,
      stages: planForm.stages,
    });
  };

  const openEditModal = (scheme) => {
    setSelectedScheme(scheme);
    setSchemeForm({
      name: scheme.name || scheme.schemeName || '',
      description: scheme.description || '',
      minIncome: scheme.minIncome !== undefined ? String(scheme.minIncome) : '0',
      maxIncome: scheme.maxIncome !== undefined ? String(scheme.maxIncome) : '300000',
      allowedCategories: scheme.allowedCategories || 'GENERAL,SC,ST,OBC,EWS',
      isActive: scheme.isActive !== undefined ? Boolean(scheme.isActive) : (scheme.active !== undefined ? Boolean(scheme.active) : true),
      requiredDocuments: scheme.requiredDocuments || 'Aadhaar,Land Record,Bank Passbook',
    });
    setEditSchemeModalOpen(true);
  };

  const pendingOfficerCount = officerRequests.filter((r) => r.status === 'PENDING').length;
  const totalBudget = schemeUtilization.length > 0
    ? schemeUtilization.reduce((sum, s) => sum + (Number(s.totalBudget) || 0), 0)
    : schemes.reduce((sum, s) => sum + (Number(s.totalBudget) || 0), 0);
  const totalUtilized = schemeUtilization.length > 0
    ? schemeUtilization.reduce((sum, s) => sum + (Number(s.utilizedBudget) || 0), 0)
    : schemes.reduce((sum, s) => sum + (Number(s.utilizedBudget) || 0), 0);

  // Computed Pipeline Stages
  const pipelineData = [
    { label: 'Submitted', count: allApplications.filter((a) => a.status === 'SUBMITTED' || a.status === 'DRAFT' || a.status === 'ELIGIBILITY_PENDING' || a.status === 'ELIGIBLE' || a.status === 'MANUAL_REVIEW_REQUIRED').length },
    { label: 'Field Verification', count: allApplications.filter((a) => a.status === 'FIELD_VERIFICATION_PENDING' || a.status === 'FIELD_APPROVED' || a.status === 'RE_VERIFICATION_REQUIRED').length },
    { label: 'District Review', count: allApplications.filter((a) => a.status === 'DISTRICT_REVIEW_PENDING' || a.status === 'DISTRICT_APPROVED').length },
    { label: 'Finance Review', count: allApplications.filter((a) => a.status === 'FINANCE_REVIEW_PENDING' || a.status === 'FINANCE_APPROVED').length },
    { label: 'Disbursed', count: allApplications.filter((a) => a.status === 'READY_FOR_DISBURSEMENT' || a.status === 'DISBURSED' || a.status === 'COMPLETED').length },
    { label: 'Rejected', count: allApplications.filter((a) => a.status === 'FIELD_REJECTED' || a.status === 'DISTRICT_REJECTED' || a.status === 'FINANCE_REJECTED' || a.status === 'NOT_ELIGIBLE' || a.status === 'APPLICATION_CANCELLED').length },
  ];

  const filteredApps = allApplications.filter((app) => {
    const matchesSearch = !appSearch ||
      String(app.id).includes(appSearch) ||
      (app.beneficiary?.fullName || '').toLowerCase().includes(appSearch.toLowerCase()) ||
      (app.scheme?.schemeName || app.scheme?.name || '').toLowerCase().includes(appSearch.toLowerCase());
    return matchesSearch;
  });

  const filteredBeneficiaries = allBeneficiaries.filter((b) => {
    const matchesSearch = !beneficiarySearch ||
      String(b.id).includes(beneficiarySearch) ||
      (b.fullName || '').toLowerCase().includes(beneficiarySearch.toLowerCase()) ||
      (b.nationalIdNumber || '').includes(beneficiarySearch) ||
      (b.region || '').toLowerCase().includes(beneficiarySearch.toLowerCase());
    return matchesSearch;
  });

  const filteredAuditLogs = auditLogs.filter((log) => {
    if (!auditSearchTerm) return true;
    const term = auditSearchTerm.toLowerCase();
    return (
      String(log.id).includes(term) ||
      String(log.entityId).includes(term) ||
      (log.entityName || '').toLowerCase().includes(term) ||
      (log.action || '').toLowerCase().includes(term) ||
      (log.actorName || '').toLowerCase().includes(term) ||
      (log.actorEmail || '').toLowerCase().includes(term) ||
      (log.details || '').toLowerCase().includes(term)
    );
  });

  const sidebarItems = [
    { type: 'heading', label: 'Admin Console' },
    { key: 'overview', label: 'Executive Dashboard', icon: LayoutDashboard },
    { key: 'officer-requests', label: 'Officer Requests', icon: UserCheck, badge: pendingOfficerCount },
    { key: 'schemes', label: 'Scheme Management', icon: FolderOpen, badge: schemes.length },
    { key: 'applications', label: 'All Applications', icon: FileText, badge: allApplications.length },
    { key: 'beneficiaries', label: 'Beneficiary Directory', icon: Users, badge: allBeneficiaries.length },
    { key: 'reports', label: 'Reports & Exports', icon: FileSpreadsheet },
    { key: 'analytics', label: 'Regional Analytics', icon: BarChart3 },
    { key: 'advanced-analytics', label: 'Advanced Analytics', icon: TrendingUp },
    { key: 'users', label: 'User Accounts', icon: ShieldCheck, badge: allUsers.length },
    { key: 'audit-logs', label: 'Audit Trail & Decisions', icon: History, badge: auditLogs.length },
  ];

  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  return (
    <div className="portal-layout" style={{ backgroundColor: 'var(--bg-main)' }}>
      <Sidebar
        items={sidebarItems}
        activeTab={activeTab}
        onSelectTab={setActiveTab}
        userRole="ADMIN"
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
                {activeTab === 'overview' && 'Executive Administration Dashboard 📊'}
                {activeTab === 'officer-requests' && 'Officer Registration Requests 👤'}
                {activeTab === 'schemes' && 'Government Subsidy Schemes 📋'}
                {activeTab === 'applications' && 'Master Application Directory 📂'}
                {activeTab === 'beneficiaries' && 'Enrolled Citizens & Beneficiary Registry 🇮🇳'}
                {activeTab === 'reports' && 'Downloadable Scheme & Regional Summaries 📥'}
                {activeTab === 'analytics' && 'Regional Analytics & Fund Absorption 📈'}
                {activeTab === 'advanced-analytics' && 'Advanced Policy & Governance Analytics 🔬'}
                {activeTab === 'users' && 'System User Directory 👥'}
                {activeTab === 'audit-logs' && 'Platform Audit Trail & Important Decisions Log 📜'}
              </h1>
              <p style={{ fontSize: '12.5px', color: '#64748b', marginTop: '3px' }}>
                Administrator: <strong>{user?.name}</strong> • Direct Beneficiary Transfer & Scheme Oversight
              </p>
            </div>
          </div>

          {activeTab === 'schemes' && (
            <button
              onClick={() => {
                setSchemePackage(defaultSchemePackage);
                setActivePackageTab('core');
                setNewSchemeModalOpen(true);
              }}
              className="btn btn-primary btn-sm"
            >
              <PlusCircle size={15} />
              <span>Create New Scheme</span>
            </button>
          )}
        </div>

        {/* TAB 1: OVERVIEW */}
        {activeTab === 'overview' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
              <MetricCard
                title="Total Enrolled Citizens"
                value={allBeneficiaries.length || allUsers.length}
                subtext="Registered beneficiaries"
                icon={Users}
                color="primary"
              />
              <MetricCard
                title="Total Applications"
                value={analyticsOverview?.totalApplications ?? allApplications.length}
                subtext="Across all schemes"
                icon={FileText}
                color="primary"
              />
              <MetricCard
                title="Total Allocated Budget"
                value={formatINR(analyticsOverview?.totalBudgetAllocated ?? totalBudget)}
                subtext="Across active schemes"
                icon={IndianRupee}
                color="success"
              />
              <MetricCard
                title="Total Budget Utilized"
                value={formatINR(analyticsOverview?.totalBudgetUtilized ?? totalUtilized)}
                subtext="Released treasury funds"
                icon={Building}
                color="primary"
              />
            </div>

            <div className="grid-analytics-2col">
              <div className="card">
                <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '6px' }}>
                  Application Pipeline Stages
                </h3>
                <p style={{ fontSize: '12.5px', color: '#64748b', marginBottom: '16px' }}>
                  Real-time status tracking of all applications across review levels.
                </p>
                <div className="chart-wrapper">
                  <PipelineStageChart pipeline={pipelineData} />
                </div>
              </div>

              <div className="card">
                <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '6px' }}>
                  Total Budget Utilization
                </h3>
                <p style={{ fontSize: '12.5px', color: '#64748b', marginBottom: '16px' }}>
                  Utilized vs remaining treasury funds across all programs.
                </p>
                <div className="chart-wrapper">
                  <BudgetUtilizationDonut
                    utilized={analyticsOverview?.totalBudgetUtilized ?? totalUtilized}
                    total={analyticsOverview?.totalBudgetAllocated ?? totalBudget}
                  />
                </div>
              </div>
            </div>

            <div className="card">
              <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '6px' }}>
                Direct Benefit Transfer (DBT) Trends
              </h3>
              <p style={{ fontSize: '12.5px', color: '#64748b', marginBottom: '16px' }}>
                Monthly direct treasury disbursement velocity to verified bank accounts.
              </p>
              <div className="chart-wrapper">
                <DisbursementTrendsChart trends={disbursementTrends} />
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: OFFICER REQUESTS */}
        {activeTab === 'officer-requests' && (
          <div className="card">
            <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '16px' }}>
              Pending Officer Requests ({pendingOfficerCount})
            </h3>
            {officerRequests.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 12px', color: '#64748b' }}>
                <CheckCircle2 size={36} color="#059669" style={{ margin: '0 auto 10px' }} />
                <p style={{ fontWeight: 700 }}>No officer registration requests pending!</p>
              </div>
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Req ID</th>
                      <th>Applicant Name</th>
                      <th>Email</th>
                      <th>Requested Role</th>
                      <th>Region</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {officerRequests.map((req) => (
                      <tr key={req.id}>
                        <td><code>#{req.id}</code></td>
                        <td><strong>{req.fullName}</strong></td>
                        <td>{req.email}</td>
                        <td><span className="badge badge-primary">{req.requestedRole}</span></td>
                        <td>{req.region}</td>
                        <td><StatusBadge status={req.status} /></td>
                        <td>
                          {req.status === 'PENDING' ? (
                            <div style={{ display: 'flex', gap: '8px' }}>
                              <button
                                onClick={() => officerRequestMutation.mutate({ id: req.id, action: 'approve' })}
                                disabled={officerRequestMutation.isPending}
                                className="btn btn-success btn-sm"
                              >
                                <CheckCircle2 size={13} />
                                <span>Approve</span>
                              </button>
                              <button
                                onClick={() => officerRequestMutation.mutate({ id: req.id, action: 'reject' })}
                                disabled={officerRequestMutation.isPending}
                                className="btn btn-danger btn-sm"
                              >
                                <XCircle size={13} />
                                <span>Reject</span>
                              </button>
                            </div>
                          ) : (
                            <span style={{ fontSize: '12px', color: '#64748b', fontWeight: 600 }}>Processed</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* TAB 3: SCHEME MANAGEMENT WITH SLABS, BUDGETS & PLANS */}
        {activeTab === 'schemes' && (
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '10px' }}>
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 700 }}>
                  Government Subsidy Schemes ({schemes.length})
                </h3>
                <p style={{ fontSize: '12.5px', color: '#64748b', marginTop: '2px' }}>
                  All scheme slabs, regional budgets, and disbursement plans are configured here by the Admin.
                </p>
              </div>
              <button
                onClick={() => {
                  setSchemePackage(defaultSchemePackage);
                  setActivePackageTab('core');
                  setNewSchemeModalOpen(true);
                }}
                className="btn btn-primary btn-sm"
              >
                <PlusCircle size={14} />
                <span>+ Create New Scheme Package</span>
              </button>
            </div>
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Scheme ID</th>
                    <th>Scheme Name</th>
                    <th>Income Range</th>
                    <th>Allowed Categories</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {schemes.map((s) => (
                    <tr key={s.id}>
                      <td><code>#{s.id}</code></td>
                      <td>
                        <strong>{s.name || s.schemeName}</strong>
                        <div style={{ fontSize: '12px', color: '#64748b' }}>{s.description?.slice(0, 60)}...</div>
                      </td>
                      <td>₹{Number(s.minIncome || 0).toLocaleString()} - ₹{Number(s.maxIncome || 300000).toLocaleString()}</td>
                      <td>
                        <span className="badge badge-primary">{s.allowedCategories || 'ALL'}</span>
                      </td>
                      <td>
                        <button
                          type="button"
                          onClick={() => toggleSchemeActiveMutation.mutate(s)}
                          disabled={toggleSchemeActiveMutation.isPending}
                          style={{
                            background: 'none',
                            border: 'none',
                            padding: 0,
                            cursor: 'pointer',
                            textAlign: 'left',
                          }}
                          title="Click to toggle Scheme Active / Inactive"
                        >
                          <span className={`badge ${(s.isActive !== false && s.active !== false) ? 'badge-success' : 'badge-danger'}`} style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                            {(s.isActive !== false && s.active !== false) ? '✓ Active' : '✕ Inactive'}
                          </span>
                        </button>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                          <button
                            onClick={() => openEditModal(s)}
                            className="btn btn-secondary btn-sm"
                            title="Edit Scheme Parameters"
                          >
                            <Edit2 size={13} />
                            <span>Edit</span>
                          </button>
                          <button
                            onClick={() => { setSelectedScheme(s); setSlabsModalOpen(true); }}
                            className="btn btn-outline btn-sm"
                            title="Manage Category Grant Slabs"
                          >
                            <Layers size={13} />
                            <span>Slabs</span>
                          </button>
                          <button
                            onClick={() => { setSelectedScheme(s); setRegionalBudgetsModalOpen(true); }}
                            className="btn btn-outline btn-sm"
                            title="Manage Regional Budgets"
                          >
                            <MapPin size={13} />
                            <span>Regions</span>
                          </button>
                          <button
                            onClick={() => { setSelectedScheme(s); setDisbursementPlanModalOpen(true); }}
                            className="btn btn-primary btn-sm"
                            title="Configure Milestone-based Disbursement Plan"
                          >
                            <IndianRupee size={13} />
                            <span>Plan</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* TAB 4: ALL APPLICATIONS WITH STATUS FILTER & DETAILS */}
        {activeTab === 'applications' && (
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 700 }}>
                Master Applications Registry ({filteredApps.length})
              </h3>

              <div style={{ display: 'flex', gap: '10px' }}>
                <input
                  type="text"
                  placeholder="Search by ID, applicant, or scheme..."
                  className="form-input"
                  style={{ width: '240px' }}
                  value={appSearch}
                  onChange={(e) => setAppSearch(e.target.value)}
                />

                <select
                  className="form-select"
                  style={{ width: '200px' }}
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                >
                  <option value="">All Application Statuses</option>
                  <option value="DRAFT">DRAFT</option>
                  <option value="SUBMITTED">SUBMITTED</option>
                  <option value="ELIGIBLE">ELIGIBLE</option>
                  <option value="FIELD_VERIFICATION_PENDING">FIELD_VERIFICATION_PENDING</option>
                  <option value="FIELD_APPROVED">FIELD_APPROVED</option>
                  <option value="DISTRICT_REVIEW_PENDING">DISTRICT_REVIEW_PENDING</option>
                  <option value="DISTRICT_APPROVED">DISTRICT_APPROVED</option>
                  <option value="FINANCE_REVIEW_PENDING">FINANCE_REVIEW_PENDING</option>
                  <option value="READY_FOR_DISBURSEMENT">READY_FOR_DISBURSEMENT</option>
                  <option value="DISBURSED">DISBURSED</option>
                  <option value="COMPLETED">COMPLETED</option>
                  <option value="NOT_ELIGIBLE">NOT_ELIGIBLE</option>
                  <option value="FIELD_REJECTED">FIELD_REJECTED</option>
                  <option value="DISTRICT_REJECTED">DISTRICT_REJECTED</option>
                </select>
              </div>
            </div>

            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>App ID</th>
                    <th>Applicant</th>
                    <th>Scheme</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Submitted On</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredApps.map((app) => (
                    <tr key={app.id}>
                      <td><code>#{app.id}</code></td>
                      <td><strong>{app.beneficiary?.fullName || 'Citizen'}</strong></td>
                      <td>{app.scheme?.schemeName || app.scheme?.name || 'Grant'}</td>
                      <td><strong>{formatINRFull(app.requestedAmount)}</strong></td>
                      <td><StatusBadge status={app.status} /></td>
                      <td>{formatDate(app.submissionDate || app.createdAt)}</td>
                      <td>
                        <button
                          onClick={() => { setSelectedAppId(app.id); setAppDetailModalOpen(true); }}
                          className="btn btn-secondary btn-sm"
                        >
                          <Eye size={13} />
                          <span>View Detail</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* TAB 5: BENEFICIARY DIRECTORY */}
        {activeTab === 'beneficiaries' && (
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 700 }}>
                  Enrolled Beneficiaries Directory ({filteredBeneficiaries.length})
                </h3>
                <p style={{ fontSize: '12.5px', color: '#64748b' }}>
                  Direct Benefit Transfer registry of citizens with verified Aadhaar credentials.
                </p>
              </div>

              <input
                type="text"
                placeholder="Search by ID, name, Aadhaar, region..."
                className="form-input"
                style={{ width: '280px' }}
                value={beneficiarySearch}
                onChange={(e) => setBeneficiarySearch(e.target.value)}
              />
            </div>

            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Beneficiary ID</th>
                    <th>Full Name</th>
                    <th>National ID (Aadhaar)</th>
                    <th>Category</th>
                    <th>Region / State</th>
                    <th>Annual Income</th>
                    <th>Phone</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredBeneficiaries.map((b) => (
                    <tr key={b.id}>
                      <td><code>#{b.id}</code></td>
                      <td><strong>{b.fullName}</strong></td>
                      <td>
                        <code>•••• •••• {b.nationalIdNumber?.slice(-4) || '1234'}</code>
                      </td>
                      <td><span className="badge badge-primary">{b.category}</span></td>
                      <td>{b.region}</td>
                      <td>{formatINRFull(b.annualIncome || 0)}</td>
                      <td>{b.phoneNumber || '—'}</td>
                      <td>
                        <button
                          onClick={() => { setSelectedBeneficiaryId(b.id); setBeneficiaryModalOpen(true); }}
                          className="btn btn-outline btn-sm"
                        >
                          <Eye size={13} />
                          <span>Profile</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* TAB 6: REPORTS & EXPORTS */}
        {activeTab === 'reports' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div className="card">
              <h3 style={{ fontSize: '17px', fontWeight: 700, marginBottom: '6px' }}>
                Downloadable Scheme & Regional Summaries
              </h3>
              <p style={{ fontSize: '13px', color: '#64748b', marginBottom: '20px' }}>
                Generate official audit-ready spreadsheets and PDF reports for parliamentary and treasury oversight.
              </p>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px' }}>
                {/* Scheme Excel Report */}
                <div style={{ border: '1px solid #e2e8f0', borderRadius: '10px', padding: '20px', backgroundColor: '#f8fafc' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
                    <div style={{ padding: '10px', borderRadius: '8px', backgroundColor: '#dcfce7', color: '#15803d' }}>
                      <FileSpreadsheet size={24} />
                    </div>
                    <div>
                      <h4 style={{ fontSize: '15px', fontWeight: 700, margin: 0 }}>Scheme Summary (Excel)</h4>
                      <span style={{ fontSize: '12px', color: '#64748b' }}>.XLSX Format • Live data</span>
                    </div>
                  </div>
                  <p style={{ fontSize: '12.5px', color: '#475569', marginBottom: '16px' }}>
                    Complete breakdown of schemes, allocations, applicant counts, and fund utilization stats.
                  </p>
                  <button
                    onClick={() => handleDownloadReport('/api/v1/reports/schemes/excel', `scheme-summary-${new Date().toISOString().slice(0,10)}.xlsx`)}
                    className="btn btn-success btn-sm"
                    style={{ width: '100%', justifyContent: 'center' }}
                  >
                    <Download size={14} />
                    <span>Download Excel Sheet</span>
                  </button>
                </div>

                {/* Scheme PDF Report */}
                <div style={{ border: '1px solid #e2e8f0', borderRadius: '10px', padding: '20px', backgroundColor: '#f8fafc' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
                    <div style={{ padding: '10px', borderRadius: '8px', backgroundColor: '#fee2e2', color: '#b91c1c' }}>
                      <FileDown size={24} />
                    </div>
                    <div>
                      <h4 style={{ fontSize: '15px', fontWeight: 700, margin: 0 }}>Scheme Summary (PDF)</h4>
                      <span style={{ fontSize: '12px', color: '#64748b' }}>.PDF Format • Official Docket</span>
                    </div>
                  </div>
                  <p style={{ fontSize: '12.5px', color: '#475569', marginBottom: '16px' }}>
                    Printable executive briefing on national subsidy performance and budget disbursement.
                  </p>
                  <button
                    onClick={() => handleDownloadReport('/api/v1/reports/schemes/pdf', `scheme-summary-${new Date().toISOString().slice(0,10)}.pdf`)}
                    className="btn btn-danger btn-sm"
                    style={{ width: '100%', justifyContent: 'center' }}
                  >
                    <Download size={14} />
                    <span>Download PDF Document</span>
                  </button>
                </div>

                {/* Regional Excel Report */}
                <div style={{ border: '1px solid #e2e8f0', borderRadius: '10px', padding: '20px', backgroundColor: '#f8fafc' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
                    <div style={{ padding: '10px', borderRadius: '8px', backgroundColor: '#e0e7ff', color: '#4338ca' }}>
                      <MapPin size={24} />
                    </div>
                    <div>
                      <h4 style={{ fontSize: '15px', fontWeight: 700, margin: 0 }}>Regional Summary (Excel)</h4>
                      <span style={{ fontSize: '12px', color: '#64748b' }}>.XLSX Format • State-wise</span>
                    </div>
                  </div>
                  <p style={{ fontSize: '12.5px', color: '#475569', marginBottom: '16px' }}>
                    State and district level grant distribution, applications volume, and regional absorption metrics.
                  </p>
                  <button
                    onClick={() => handleDownloadReport('/api/v1/reports/regions/excel', `regional-summary-${new Date().toISOString().slice(0,10)}.xlsx`)}
                    className="btn btn-primary btn-sm"
                    style={{ width: '100%', justifyContent: 'center' }}
                  >
                    <Download size={14} />
                    <span>Download Regional Excel</span>
                  </button>
                </div>

                {/* Regional PDF Report */}
                <div style={{ border: '1px solid #e2e8f0', borderRadius: '10px', padding: '20px', backgroundColor: '#f8fafc' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
                    <div style={{ padding: '10px', borderRadius: '8px', backgroundColor: '#fef3c7', color: '#b45309' }}>
                      <FileText size={24} />
                    </div>
                    <div>
                      <h4 style={{ fontSize: '15px', fontWeight: 700, margin: 0 }}>Regional Summary (PDF)</h4>
                      <span style={{ fontSize: '12px', color: '#64748b' }}>.PDF Format • Spatial Analysis</span>
                    </div>
                  </div>
                  <p style={{ fontSize: '12.5px', color: '#475569', marginBottom: '16px' }}>
                    State-wise comparative analysis report with regional fund utilization indices.
                  </p>
                  <button
                    onClick={() => handleDownloadReport('/api/v1/reports/regions/pdf', `regional-summary-${new Date().toISOString().slice(0,10)}.pdf`)}
                    className="btn btn-warning btn-sm"
                    style={{ width: '100%', justifyContent: 'center' }}
                  >
                    <Download size={14} />
                    <span>Download Regional PDF</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 7: REGIONAL ANALYTICS */}
        {activeTab === 'analytics' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            <div className="grid-analytics-equal">
              <div className="card">
                <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '6px' }}>
                  Regional Applications vs Approved Grants
                </h3>
                <p style={{ fontSize: '12.5px', color: '#64748b', marginBottom: '16px' }}>
                  Comparison of total applicants vs sanctioned subsidies by State / District.
                </p>
                <div className="chart-wrapper">
                  <RegionalApplicationsChart regions={regionalData} />
                </div>
              </div>

              <div className="card">
                <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '6px' }}>
                  Regional Budget Absorption Rate (%)
                </h3>
                <p style={{ fontSize: '12.5px', color: '#64748b', marginBottom: '16px' }}>
                  Percentage of regional fund allocation absorbed.
                </p>
                <div className="chart-wrapper">
                  <RegionalUtilizationChart regions={regionalData} />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 8: ADVANCED ANALYTICS */}
        {activeTab === 'advanced-analytics' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            {/* Turnaround & Category Cards */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '16px' }}>
              <MetricCard
                title="Avg Approval Turnaround"
                value={`${turnaroundData?.averageDays !== undefined ? Number(turnaroundData.averageDays).toFixed(1) : '0.0'} Days`}
                subtext={`Fastest: ${turnaroundData?.fastestDays !== undefined ? Number(turnaroundData.fastestDays).toFixed(1) : '0.0'}d • Max: ${turnaroundData?.slowestDays !== undefined ? Number(turnaroundData.slowestDays).toFixed(1) : '0.0'}d`}
                icon={Clock}
                color="primary"
              />
              <MetricCard
                title="Active Budget Warnings"
                value={budgetWarnings.length}
                subtext="Programs exceeding 80% absorption"
                icon={AlertOctagon}
                color={budgetWarnings.length > 0 ? 'warning' : 'success'}
              />
              <MetricCard
                title="Active Government Schemes"
                value={schemes.filter(s => s.isActive !== false && s.active !== false).length}
                subtext={`${schemes.length} total schemes registered`}
                icon={FolderOpen}
                color="primary"
              />
            </div>

            {/* Social Category Inclusivity Analytics Graph & Breakdown */}
            <div className="grid-analytics-inclusivity">
              <div className="card">
                <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '6px' }}>
                  Social Inclusivity & Demographic Spread 📊
                </h3>
                <p style={{ fontSize: '12.5px', color: '#64748b', marginBottom: '16px' }}>
                  Citizen beneficiary representation across social categories (GENERAL, OBC, SC, ST, EWS).
                </p>
                <div className="chart-wrapper">
                  <CategoryInclusivityChart categories={categoryDistributions} />
                </div>
              </div>

              <div className="card">
                <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '12px' }}>
                  Category Representation Breakdown
                </h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '12px' }}>
                  {categoryDistributions.map((c, idx) => (
                    <div key={idx} style={{ padding: '14px', border: '1px solid #e2e8f0', borderRadius: '8px', backgroundColor: '#f8fafc' }}>
                      <div style={{ fontSize: '11.5px', color: '#64748b', fontWeight: 600 }}>Category</div>
                      <div style={{ fontSize: '17px', fontWeight: 800, color: '#0f172a', margin: '3px 0' }}>{c.category}</div>
                      <div style={{ fontSize: '13px', color: '#059669', fontWeight: 700 }}>
                        {c.count ?? c.beneficiaryCount ?? 0} Citizens
                      </div>
                      <div style={{ fontSize: '11.5px', color: '#64748b', marginTop: '2px' }}>
                        {Number(c.percent ?? c.percentage ?? 0).toFixed(1)}% Total
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Budget Exhaustion Warnings */}
            <div className="card">
              <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '12px' }}>
                Budget Exhaustion Warnings
              </h3>
              {budgetWarnings.length === 0 ? (
                <div style={{ padding: '24px', textAlign: 'center', color: '#059669', fontWeight: 600 }}>
                  ✓ All program budgets are operating within healthy allocation limits.
                </div>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Scheme / Region</th>
                        <th>Allocated Budget</th>
                        <th>Utilized</th>
                        <th>Utilization %</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {budgetWarnings.map((w, idx) => (
                        <tr key={idx}>
                          <td><strong>{w.schemeName || w.regionName}</strong></td>
                          <td>{formatINR(w.allocatedBudget)}</td>
                          <td>{formatINR(w.utilizedBudget)}</td>
                          <td><strong>{Number(w.utilizationPercentage).toFixed(1)}%</strong></td>
                          <td><span className="badge badge-danger">High Absorption</span></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}


        {/* TAB 10: USER ACCOUNTS */}
        {activeTab === 'users' && (
          <div className="card">
            <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '16px' }}>
              System User Accounts ({allUsers.length})
            </h3>
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>User ID</th>
                    <th>Full Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Jurisdiction</th>
                  </tr>
                </thead>
                <tbody>
                  {allUsers.map((u) => (
                    <tr key={u.id}>
                      <td><code>#{u.id}</code></td>
                      <td><strong>{u.fullName}</strong></td>
                      <td>{u.email}</td>
                      <td><span className="badge badge-primary">{u.role}</span></td>
                      <td>{u.region || 'ALL'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* TAB 11: AUDIT TRAIL & DECISION HISTORY */}
        {activeTab === 'audit-logs' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* Header / Filter Toolbar */}
            <div className="card" style={{ padding: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
                <div>
                  <h3 style={{ fontSize: '18px', fontWeight: 700, color: '#0f172a' }}>
                    Platform Audit Trail & State Transitions
                  </h3>
                  <p style={{ fontSize: '13px', color: '#64748b', marginTop: '2px' }}>
                    Immutable historical record of every application submission, KYC document verification, sanction decision, and tranche disbursement.
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

              {/* Filters */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px' }}>
                <div>
                  <label className="form-label" style={{ fontSize: '12px' }}>Filter by Entity</label>
                  <select
                    className="form-select"
                    value={auditEntityFilter}
                    onChange={(e) => setAuditEntityFilter(e.target.value)}
                    style={{ fontSize: '13px' }}
                  >
                    <option value="">All Entities</option>
                    <option value="Application">Application</option>
                    <option value="Document">Document</option>
                    <option value="DisbursementSchedule">Disbursement Schedule</option>
                    <option value="ComplianceMilestone">Compliance Milestone</option>
                    <option value="User">User / Officer</option>
                  </select>
                </div>

                <div>
                  <label className="form-label" style={{ fontSize: '12px' }}>Filter by Action</label>
                  <select
                    className="form-select"
                    value={auditActionFilter}
                    onChange={(e) => setAuditActionFilter(e.target.value)}
                    style={{ fontSize: '13px' }}
                  >
                    <option value="">All Actions</option>
                    <option value="SUBMITTED">SUBMITTED</option>
                    <option value="DOCUMENT_VERIFIED">DOCUMENT_VERIFIED</option>
                    <option value="DOCUMENT_REJECTED">DOCUMENT_REJECTED</option>
                    <option value="APPROVED">APPROVED</option>
                    <option value="REJECTED">REJECTED</option>
                    <option value="SCHEDULE_GENERATED">SCHEDULE_GENERATED</option>
                    <option value="STATUS_UPDATED">STATUS_UPDATED</option>
                    <option value="TREASURY_DISPATCHED">TREASURY_DISPATCHED</option>
                    <option value="MILESTONE_COMPLETED">MILESTONE_COMPLETED</option>
                    <option value="DAILY_OVERDUE_CHECK">DAILY_OVERDUE_CHECK</option>
                    <option value="OFFICER_APPROVED">OFFICER_APPROVED</option>
                    <option value="OFFICER_REJECTED">OFFICER_REJECTED</option>
                  </select>
                </div>

                <div>
                  <label className="form-label" style={{ fontSize: '12px' }}>Search Actor / Details</label>
                  <div style={{ position: 'relative' }}>
                    <Search size={15} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
                    <input
                      type="text"
                      className="form-input"
                      placeholder="Search by ID, name, remarks..."
                      value={auditSearchTerm}
                      onChange={(e) => setAuditSearchTerm(e.target.value)}
                      style={{ paddingLeft: '32px', fontSize: '13px' }}
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Audit Log Table */}
            <div className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                <h4 style={{ fontSize: '15px', fontWeight: 700 }}>
                  Audit Events ({filteredAuditLogs.length})
                </h4>
              </div>

              {auditLogsLoading ? (
                <div style={{ padding: '32px', textAlign: 'center', color: '#64748b' }}>
                  Loading audit logs...
                </div>
              ) : filteredAuditLogs.length === 0 ? (
                <div style={{ padding: '32px', textAlign: 'center', color: '#64748b' }}>
                  No audit logs matching selected filters.
                </div>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Log ID</th>
                        <th>Timestamp</th>
                        <th>Action</th>
                        <th>Target Entity</th>
                        <th>Actor</th>
                        <th>Decision / Remarks Details</th>
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
                              log.action?.includes('APPROV') || log.action?.includes('VERIF') || log.action?.includes('COMPLET') || log.action?.includes('DISPATCH')
                                ? 'badge-success'
                                : log.action?.includes('REJECT')
                                ? 'badge-danger'
                                : 'badge-primary'
                            }`}>
                              {log.action}
                            </span>
                          </td>
                          <td>
                            <span style={{ fontSize: '12.5px', fontWeight: 600 }}>
                              {log.entityName}
                            </span>
                            <span style={{ fontSize: '11px', color: '#64748b', marginLeft: '6px' }}>
                              #{log.entityId}
                            </span>
                          </td>
                          <td>
                            <div style={{ display: 'flex', flexDirection: 'column' }}>
                              <span style={{ fontSize: '13px', fontWeight: 600 }}>{log.actorName || 'System'}</span>
                              <span style={{ fontSize: '11px', color: '#64748b' }}>
                                {log.actorRole || 'SYSTEM'} {log.actorEmail ? `• ${log.actorEmail}` : ''}
                              </span>
                            </div>
                          </td>
                          <td style={{ maxWidth: '380px' }}>
                            <div style={{ fontSize: '12.5px', color: '#334155', wordBreak: 'break-word', lineHeight: '1.4' }}>
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

      {/* 1. Unified Scheme Creation Package Modal (Admin Manual Configuration) */}
      <Modal
        isOpen={newSchemeModalOpen}
        onClose={() => setNewSchemeModalOpen(false)}
        title="Create & Configure Government Grant Scheme"
        subtitle="Admin setup: Core parameters, category slabs, regional budgets, and dynamic disbursement plan"
      >
        <form onSubmit={handleCreateSchemePackage}>
          {/* Step Navigation Tabs */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 110px), 1fr))',
            gap: '8px',
            marginBottom: '20px',
            borderBottom: '1px solid #e2e8f0',
            paddingBottom: '12px'
          }}>
            {[
              { key: 'core', label: '1. Core & Docs' },
              { key: 'slabs', label: `2. Slabs (${schemePackage.slabs.length})` },
              { key: 'regions', label: `3. Budgets (${schemePackage.regionalBudgets.length})` },
              { key: 'plan', label: `4. Plan (${schemePackage.stages.length} Stages)` },
            ].map((tab) => (
              <button
                key={tab.key}
                type="button"
                onClick={() => setActivePackageTab(tab.key)}
                className={`btn btn-sm ${activePackageTab === tab.key ? 'btn-primary' : 'btn-outline'}`}
                style={{ justifyContent: 'center', fontSize: '12px', padding: '6px 8px' }}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* TAB 1: CORE SCHEME DETAILS & REQUIRED DOCUMENTS */}
          {activePackageTab === 'core' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div className="form-group">
                <label className="form-label">Scheme Official Name <span className="req">*</span></label>
                <input
                  type="text"
                  required
                  placeholder="e.g. National Solar Agri-Pump Subsidy"
                  className="form-input"
                  value={schemePackage.name}
                  onChange={(e) => setSchemePackage({ ...schemePackage, name: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Description & Scope <span className="req">*</span></label>
                <textarea
                  required
                  rows={3}
                  placeholder="Provides capital subsidy for solar irrigation pumps to farmers..."
                  className="form-textarea"
                  value={schemePackage.description}
                  onChange={(e) => setSchemePackage({ ...schemePackage, description: e.target.value })}
                />
              </div>

              <div className="modal-form-grid-2">
                <div className="form-group">
                  <label className="form-label">Min Annual Income (₹)</label>
                  <input
                    type="number"
                    min="0"
                    className="form-input"
                    value={schemePackage.minIncome}
                    onChange={(e) => setSchemePackage({ ...schemePackage, minIncome: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Max Annual Income Ceiling (₹)</label>
                  <input
                    type="number"
                    min="0"
                    className="form-input"
                    value={schemePackage.maxIncome}
                    onChange={(e) => setSchemePackage({ ...schemePackage, maxIncome: e.target.value })}
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Allowed Social Categories (comma-separated)</label>
                <input
                  type="text"
                  className="form-input"
                  value={schemePackage.allowedCategories}
                  onChange={(e) => setSchemePackage({ ...schemePackage, allowedCategories: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label className="form-label">
                  Required Documents (comma-separated) <span className="req">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Aadhaar Card, Land Record, Bank Passbook"
                  className="form-input"
                  value={schemePackage.requiredDocuments}
                  onChange={(e) => setSchemePackage({ ...schemePackage, requiredDocuments: e.target.value })}
                />
                <div style={{ fontSize: '12px', color: '#0369a1', backgroundColor: '#f0f9ff', padding: '8px 12px', borderRadius: '6px', marginTop: '6px', border: '1px solid #bae6fd' }}>
                  <strong>Mandatory Rule:</strong> Beneficiaries will only be asked to upload these exact documents. There will be no dropdown menu on their portal.
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: CATEGORY GRANT SLABS */}
          {activePackageTab === 'slabs' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <h4 style={{ fontSize: '14px', fontWeight: 700 }}>Category-wise Grant Slabs</h4>
                <p style={{ fontSize: '12px', color: '#64748b', marginTop: '2px' }}>
                  Configure differential grant amounts based on beneficiary social category. Others must follow these amounts.
                </p>
              </div>

              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Citizen Category</th>
                      <th>Grant Amount (₹)</th>
                      <th style={{ width: '80px' }}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {schemePackage.slabs.map((s, idx) => (
                      <tr key={s.category + idx}>
                        <td><span className="badge badge-primary">{s.category}</span></td>
                        <td>
                          <input
                            type="number"
                            min="1000"
                            required
                            className="form-input"
                            style={{ maxWidth: '180px', padding: '4px 8px', fontSize: '13px' }}
                            value={s.grantAmount}
                            onChange={(e) => {
                              const nextSlabs = [...schemePackage.slabs];
                              nextSlabs[idx].grantAmount = e.target.value;
                              setSchemePackage({ ...schemePackage, slabs: nextSlabs });
                            }}
                          />
                        </td>
                        <td>
                          <button
                            type="button"
                            onClick={() => {
                              const nextSlabs = schemePackage.slabs.filter((_, i) => i !== idx);
                              setSchemePackage({ ...schemePackage, slabs: nextSlabs });
                            }}
                            className="btn btn-danger btn-sm"
                            style={{ padding: '4px 6px' }}
                            title="Remove Slab"
                          >
                            <Trash2 size={13} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Add New Slab */}
              <div style={{ display: 'flex', gap: '10px', alignItems: 'center', backgroundColor: '#f8fafc', padding: '10px', borderRadius: '6px', border: '1px solid #e2e8f0' }}>
                <select
                  className="form-select"
                  style={{ width: '160px', fontSize: '12.5px' }}
                  value={newSlabInput.category}
                  onChange={(e) => setNewSlabInput({ ...newSlabInput, category: e.target.value })}
                >
                  {BENEFICIARY_CATEGORIES.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
                <input
                  type="number"
                  placeholder="Grant Amount (₹)"
                  className="form-input"
                  style={{ flex: 1, fontSize: '12.5px' }}
                  value={newSlabInput.grantAmount}
                  onChange={(e) => setNewSlabInput({ ...newSlabInput, grantAmount: e.target.value })}
                />
                <button
                  type="button"
                  onClick={() => {
                    if (!newSlabInput.grantAmount || Number(newSlabInput.grantAmount) <= 0) {
                      toast.error('Enter a valid grant amount');
                      return;
                    }
                    if (schemePackage.slabs.some(s => s.category === newSlabInput.category)) {
                      toast.error(`Category ${newSlabInput.category} already has a slab configured`);
                      return;
                    }
                    setSchemePackage({
                      ...schemePackage,
                      slabs: [...schemePackage.slabs, { ...newSlabInput }]
                    });
                    setNewSlabInput({ category: 'GENERAL', grantAmount: '' });
                  }}
                  className="btn btn-secondary btn-sm"
                >
                  + Add Slab
                </button>
              </div>
            </div>
          )}

          {/* TAB 3: REGIONAL BUDGET ALLOCATIONS */}
          {activePackageTab === 'regions' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <h4 style={{ fontSize: '14px', fontWeight: 700 }}>Regional Treasury Allocations</h4>
                <p style={{ fontSize: '12px', color: '#64748b', marginTop: '2px' }}>
                  Allocate state/regional budget quotas to enable local district officers to sanction applications.
                </p>
              </div>

              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>State / Region</th>
                      <th>Allocated Budget (₹)</th>
                      <th style={{ width: '80px' }}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {schemePackage.regionalBudgets.map((b, idx) => (
                      <tr key={b.regionName + idx}>
                        <td><strong>{b.regionName}</strong></td>
                        <td>
                          <input
                            type="number"
                            min="10000"
                            required
                            className="form-input"
                            style={{ maxWidth: '180px', padding: '4px 8px', fontSize: '13px' }}
                            value={b.allocatedBudget}
                            onChange={(e) => {
                              const nextBudgets = [...schemePackage.regionalBudgets];
                              nextBudgets[idx].allocatedBudget = e.target.value;
                              setSchemePackage({ ...schemePackage, regionalBudgets: nextBudgets });
                            }}
                          />
                        </td>
                        <td>
                          <button
                            type="button"
                            onClick={() => {
                              const nextBudgets = schemePackage.regionalBudgets.filter((_, i) => i !== idx);
                              setSchemePackage({ ...schemePackage, regionalBudgets: nextBudgets });
                            }}
                            className="btn btn-danger btn-sm"
                            style={{ padding: '4px 6px' }}
                            title="Remove Region"
                          >
                            <Trash2 size={13} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Add New Region */}
              <div style={{ display: 'flex', gap: '10px', alignItems: 'center', backgroundColor: '#f8fafc', padding: '10px', borderRadius: '6px', border: '1px solid #e2e8f0' }}>
                <select
                  className="form-select"
                  style={{ width: '180px', fontSize: '12.5px' }}
                  value={newRegionInput.regionName}
                  onChange={(e) => setNewRegionInput({ ...newRegionInput, regionName: e.target.value })}
                >
                  {INDIAN_STATES.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
                <input
                  type="number"
                  placeholder="Budget Allocation (₹)"
                  className="form-input"
                  style={{ flex: 1, fontSize: '12.5px' }}
                  value={newRegionInput.allocatedBudget}
                  onChange={(e) => setNewRegionInput({ ...newRegionInput, allocatedBudget: e.target.value })}
                />
                <button
                  type="button"
                  onClick={() => {
                    if (!newRegionInput.allocatedBudget || Number(newRegionInput.allocatedBudget) <= 0) {
                      toast.error('Enter a valid budget amount');
                      return;
                    }
                    if (schemePackage.regionalBudgets.some(b => b.regionName === newRegionInput.regionName)) {
                      toast.error(`Region ${newRegionInput.regionName} already has an allocation configured`);
                      return;
                    }
                    setSchemePackage({
                      ...schemePackage,
                      regionalBudgets: [...schemePackage.regionalBudgets, { ...newRegionInput }]
                    });
                    setNewRegionInput({ regionName: 'Rajasthan', allocatedBudget: '' });
                  }}
                  className="btn btn-secondary btn-sm"
                >
                  + Add Region
                </button>
              </div>
            </div>
          )}

          {/* TAB 4: DYNAMIC DISBURSEMENT PLAN (DECIDED BY ADMIN) */}
          {activePackageTab === 'plan' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
                <div>
                  <h4 style={{ fontSize: '14px', fontWeight: 700 }}>Dynamic Staged Disbursement Plan</h4>
                  <p style={{ fontSize: '12px', color: '#64748b', marginTop: '2px' }}>
                    The admin decides the number of stages, percentages, and milestone triggers. Others must follow this plan.
                  </p>
                </div>

                {(() => {
                  const totalPct = schemePackage.stages.reduce((sum, s) => sum + (Number(s.percentageOfGrant) || 0), 0);
                  const isValid = Math.round(totalPct) === 100;
                  return (
                    <span className={`badge ${isValid ? 'badge-success' : 'badge-danger'}`} style={{ fontSize: '13px', padding: '6px 12px' }}>
                      {isValid ? '✓ Total: 100% (Valid)' : `⚠ Total: ${totalPct}% (Must equal 100%)`}
                    </span>
                  );
                })()}
              </div>

              {/* Dynamic Stages List */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {schemePackage.stages.map((st, idx) => (
                  <div
                    key={idx}
                    style={{
                      border: '1px solid #e2e8f0',
                      borderRadius: '8px',
                      padding: '12px 14px',
                      backgroundColor: '#ffffff',
                      boxShadow: '0 1px 2px rgba(0,0,0,0.02)'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <span style={{ fontWeight: 700, fontSize: '13.5px', color: '#1e293b' }}>
                        Stage {idx + 1}
                      </span>
                      {schemePackage.stages.length > 1 && (
                        <button
                          type="button"
                          onClick={() => {
                            const nextStages = schemePackage.stages.filter((_, i) => i !== idx);
                            setSchemePackage({ ...schemePackage, stages: nextStages });
                          }}
                          className="btn btn-outline btn-sm"
                          style={{ color: '#ef4444', borderColor: '#fca5a5', padding: '2px 6px', fontSize: '11px' }}
                          title="Remove Stage"
                        >
                          <Trash2 size={12} />
                          <span>Remove</span>
                        </button>
                      )}
                    </div>

                    <div className="modal-stage-grid">
                      <div>
                        <label className="form-label" style={{ fontSize: '11px' }}>Stage Name</label>
                        <input
                          type="text"
                          required
                          className="form-input"
                          style={{ fontSize: '12.5px', padding: '6px 8px' }}
                          value={st.stageName}
                          onChange={(e) => {
                            const nextStages = [...schemePackage.stages];
                            nextStages[idx].stageName = e.target.value;
                            setSchemePackage({ ...schemePackage, stages: nextStages });
                          }}
                        />
                      </div>

                      <div>
                        <label className="form-label" style={{ fontSize: '11px' }}>% of Grant</label>
                        <input
                          type="number"
                          required
                          min="1"
                          max="100"
                          className="form-input"
                          style={{ fontSize: '12.5px', padding: '6px 8px' }}
                          value={st.percentageOfGrant}
                          onChange={(e) => {
                            const nextStages = [...schemePackage.stages];
                            nextStages[idx].percentageOfGrant = parseFloat(e.target.value) || 0;
                            setSchemePackage({ ...schemePackage, stages: nextStages });
                          }}
                        />
                      </div>

                      <div>
                        <label className="form-label" style={{ fontSize: '11px' }}>Milestone Trigger</label>
                        <select
                          className="form-select"
                          style={{ fontSize: '12px', padding: '6px 8px' }}
                          value={st.triggerMilestone}
                          onChange={(e) => {
                            const nextStages = [...schemePackage.stages];
                            nextStages[idx].triggerMilestone = e.target.value;
                            setSchemePackage({ ...schemePackage, stages: nextStages });
                          }}
                        >
                          <option value="DOCUMENT_VERIFIED">DOCUMENT_VERIFIED</option>
                          <option value="FIELD_INSPECTION_PASSED">FIELD_INSPECTION_PASSED</option>
                          <option value="PROJECT_COMPLETED">PROJECT_COMPLETED</option>
                          <option value="STAGE_UTILIZATION_PROOF">STAGE_UTILIZATION_PROOF</option>
                        </select>
                      </div>

                      <div>
                        <label className="form-label" style={{ fontSize: '11px' }}>Offset (Days)</label>
                        <input
                          type="number"
                          required
                          min="0"
                          className="form-input"
                          style={{ fontSize: '12.5px', padding: '6px 8px' }}
                          value={st.dueDateOffsetDays}
                          onChange={(e) => {
                            const nextStages = [...schemePackage.stages];
                            nextStages[idx].dueDateOffsetDays = parseInt(e.target.value) || 0;
                            setSchemePackage({ ...schemePackage, stages: nextStages });
                          }}
                        />
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
                <button
                  type="button"
                  onClick={() => {
                    const nextStages = [
                      ...schemePackage.stages,
                      {
                        stageName: `Stage ${schemePackage.stages.length + 1} Milestone`,
                        percentageOfGrant: 0,
                        triggerMilestone: 'STAGE_UTILIZATION_PROOF',
                        dueDateOffsetDays: 30,
                      },
                    ];
                    setSchemePackage({ ...schemePackage, stages: nextStages });
                  }}
                  className="btn btn-secondary btn-sm"
                >
                  <PlusCircle size={13} />
                  <span>+ Add Another Disbursement Stage</span>
                </button>
              </div>
            </div>
          )}

          {/* Modal Footer Controls */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderTop: '1px solid #e2e8f0',
            paddingTop: '16px',
            marginTop: '20px'
          }}>
            <div>
              {activePackageTab !== 'core' && (
                <button
                  type="button"
                  onClick={() => {
                    if (activePackageTab === 'slabs') setActivePackageTab('core');
                    else if (activePackageTab === 'regions') setActivePackageTab('slabs');
                    else if (activePackageTab === 'plan') setActivePackageTab('regions');
                  }}
                  className="btn btn-outline btn-sm"
                >
                  ← Previous Section
                </button>
              )}
            </div>

            <div style={{ display: 'flex', gap: '8px' }}>
              {activePackageTab !== 'plan' ? (
                <button
                  type="button"
                  onClick={() => {
                    if (activePackageTab === 'core') setActivePackageTab('slabs');
                    else if (activePackageTab === 'slabs') setActivePackageTab('regions');
                    else if (activePackageTab === 'regions') setActivePackageTab('plan');
                  }}
                  className="btn btn-secondary btn-sm"
                >
                  Next Section →
                </button>
              ) : null}

              <button
                type="submit"
                disabled={isPublishingPackage}
                className="btn btn-primary btn-sm"
                style={{ padding: '8px 16px' }}
              >
                {isPublishingPackage ? 'Publishing Package...' : 'Publish Scheme & Complete Configuration →'}
              </button>
            </div>
          </div>
        </form>
      </Modal>

      {/* 2. Edit Scheme Modal */}
      <Modal
        isOpen={editSchemeModalOpen}
        onClose={() => setEditSchemeModalOpen(false)}
        title={`Edit Scheme: ${selectedScheme?.name || selectedScheme?.schemeName}`}
        subtitle="Modify income thresholds, criteria and activation state"
      >
        <form onSubmit={handleUpdateScheme}>
          <div className="form-group">
            <label className="form-label">Scheme Official Name <span className="req">*</span></label>
            <input
              type="text"
              required
              className="form-input"
              value={schemeForm.name}
              onChange={(e) => setSchemeForm({ ...schemeForm, name: e.target.value })}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Description <span className="req">*</span></label>
            <textarea
              required
              rows={3}
              className="form-textarea"
              value={schemeForm.description}
              onChange={(e) => setSchemeForm({ ...schemeForm, description: e.target.value })}
            />
          </div>

          <div className="modal-form-grid-2">
            <div className="form-group">
              <label className="form-label">Min Annual Income (₹)</label>
              <input
                type="number"
                min="0"
                className="form-input"
                value={schemeForm.minIncome}
                onChange={(e) => setSchemeForm({ ...schemeForm, minIncome: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Max Annual Income Ceiling (₹)</label>
              <input
                type="number"
                min="0"
                className="form-input"
                value={schemeForm.maxIncome}
                onChange={(e) => setSchemeForm({ ...schemeForm, maxIncome: e.target.value })}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Allowed Categories</label>
            <input
              type="text"
              className="form-input"
              value={schemeForm.allowedCategories}
              onChange={(e) => setSchemeForm({ ...schemeForm, allowedCategories: e.target.value })}
            />
          </div>

          <div className="form-group">
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={schemeForm.isActive}
                onChange={(e) => setSchemeForm({ ...schemeForm, isActive: e.target.checked })}
              />
              <span style={{ fontWeight: 600, fontSize: '14px' }}>Scheme Active & Accepting Applications</span>
            </label>
          </div>

          <button
            type="submit"
            disabled={updateSchemeMutation.isPending}
            className="btn btn-primary btn-lg"
            style={{ width: '100%', justifyContent: 'center', marginTop: '10px' }}
          >
            {updateSchemeMutation.isPending ? 'Updating...' : 'Save Scheme Changes →'}
          </button>
        </form>
      </Modal>

      {/* 3. Scheme Slabs Modal */}
      <Modal
        isOpen={slabsModalOpen}
        onClose={() => setSlabsModalOpen(false)}
        title={`Grant Slabs for ${selectedScheme?.name || selectedScheme?.schemeName}`}
        subtitle="Configure differential grant amounts based on beneficiary category"
      >
        <div style={{ marginBottom: '20px' }}>
          <h4 style={{ fontSize: '14px', fontWeight: 700, marginBottom: '8px' }}>Existing Slabs</h4>
          {schemeSlabs.length === 0 ? (
            <p style={{ fontSize: '13px', color: '#64748b' }}>No category-specific slabs defined yet. Flat grant applies.</p>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Category</th>
                  <th>Sanctioned Grant Amount</th>
                </tr>
              </thead>
              <tbody>
                {schemeSlabs.map((s) => (
                  <tr key={s.id}>
                    <td><span className="badge badge-primary">{s.category}</span></td>
                    <td><strong>{formatINRFull(s.grantAmount)}</strong></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <form onSubmit={handleAddSlab} style={{ borderTop: '1px solid #e2e8f0', paddingTop: '16px' }}>
          <h4 style={{ fontSize: '14px', fontWeight: 700, marginBottom: '12px' }}>Add New Grant Slab</h4>
          <div className="modal-form-grid-2">
            <div className="form-group">
              <label className="form-label">Citizen Category <span className="req">*</span></label>
              <select
                className="form-select"
                value={slabForm.category}
                onChange={(e) => setSlabForm({ ...slabForm, category: e.target.value })}
              >
                {BENEFICIARY_CATEGORIES.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Grant Amount (₹) <span className="req">*</span></label>
              <input
                type="number"
                required
                min="1000"
                placeholder="e.g. 75000"
                className="form-input"
                value={slabForm.grantAmount}
                onChange={(e) => setSlabForm({ ...slabForm, grantAmount: e.target.value })}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={addSlabMutation.isPending}
            className="btn btn-primary btn-sm"
            style={{ width: '100%', justifyContent: 'center' }}
          >
            {addSlabMutation.isPending ? 'Adding Slab...' : '+ Add Grant Slab'}
          </button>
        </form>
      </Modal>

      {/* 4. Regional Budgets Modal */}
      <Modal
        isOpen={regionalBudgetsModalOpen}
        onClose={() => setRegionalBudgetsModalOpen(false)}
        title={`Regional Allocations for ${selectedScheme?.name || selectedScheme?.schemeName}`}
        subtitle="Manage state/district budget allocations and monitor absorption"
      >
        <div style={{ marginBottom: '20px' }}>
          <h4 style={{ fontSize: '14px', fontWeight: 700, marginBottom: '8px' }}>Allocated Budgets</h4>
          {schemeRegionalBudgets.length === 0 ? (
            <p style={{ fontSize: '13px', color: '#64748b' }}>No regional budgets allocated yet.</p>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Region Name</th>
                  <th>Allocated Budget</th>
                  <th>Utilized Budget</th>
                </tr>
              </thead>
              <tbody>
                {schemeRegionalBudgets.map((b) => (
                  <tr key={b.id}>
                    <td><strong>{b.regionName}</strong></td>
                    <td><strong>{formatINR(b.allocatedBudget)}</strong></td>
                    <td><span style={{ color: '#059669', fontWeight: 700 }}>{formatINR(b.utilizedBudget || 0)}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <form onSubmit={handleAddRegionalBudget} style={{ borderTop: '1px solid #e2e8f0', paddingTop: '16px' }}>
          <h4 style={{ fontSize: '14px', fontWeight: 700, marginBottom: '12px' }}>Allocate Budget to Region</h4>
          <div className="modal-form-grid-2">
            <div className="form-group">
              <label className="form-label">State / Region <span className="req">*</span></label>
              <select
                className="form-select"
                value={regionalBudgetForm.regionName}
                onChange={(e) => setRegionalBudgetForm({ ...regionalBudgetForm, regionName: e.target.value })}
              >
                {INDIAN_STATES.map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Budget Allocation (₹) <span className="req">*</span></label>
              <input
                type="number"
                required
                min="10000"
                placeholder="e.g. 5000000"
                className="form-input"
                value={regionalBudgetForm.allocatedBudget}
                onChange={(e) => setRegionalBudgetForm({ ...regionalBudgetForm, allocatedBudget: e.target.value })}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={addRegionalBudgetMutation.isPending}
            className="btn btn-primary btn-sm"
            style={{ width: '100%', justifyContent: 'center' }}
          >
            {addRegionalBudgetMutation.isPending ? 'Allocating...' : '+ Allocate Regional Budget'}
          </button>
        </form>
      </Modal>

      {/* 5. Disbursement Plan Modal */}
      <Modal
        isOpen={disbursementPlanModalOpen}
        onClose={() => setDisbursementPlanModalOpen(false)}
        title={`Disbursement Plan: ${selectedScheme?.name || selectedScheme?.schemeName}`}
        subtitle="Configure tranche releases tied to verified ground compliance milestones"
      >
        {schemeDisbursementPlan ? (
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
              <div>
                <span className="badge badge-success">Plan Configured</span>
                <span style={{ fontSize: '13px', color: '#64748b', marginLeft: '8px' }}>
                  {schemeDisbursementPlan.numberOfStages} Tranche Stages
                </span>
              </div>
              <button
                onClick={() => deletePlanMutation.mutate(schemeDisbursementPlan.id)}
                disabled={deletePlanMutation.isPending}
                className="btn btn-danger btn-sm"
              >
                <Trash2 size={13} />
                <span>Delete Plan</span>
              </button>
            </div>

            <table className="data-table" style={{ marginBottom: '20px' }}>
              <thead>
                <tr>
                  <th>Seq</th>
                  <th>Stage Name</th>
                  <th>% of Grant</th>
                  <th>Trigger Milestone</th>
                  <th>Offset Days</th>
                </tr>
              </thead>
              <tbody>
                {schemeDisbursementPlan.stages?.map((st) => (
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
        ) : (
          <form onSubmit={handleSaveDisbursementPlan}>
            <p style={{ fontSize: '13px', color: '#64748b', marginBottom: '16px' }}>
              No disbursement plan configured for this scheme yet. Set up the milestone-triggered disbursement schedule:
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '16px' }}>
              {planForm.stages.map((stage, idx) => (
                <div key={idx} style={{ padding: '12px', border: '1px solid #e2e8f0', borderRadius: '8px', backgroundColor: '#f8fafc' }}>
                  <div className="modal-stage-grid">
                    <div>
                      <label style={{ fontSize: '11px', color: '#64748b' }}>Stage Name</label>
                      <input
                        type="text"
                        className="form-input"
                        value={stage.stageName}
                        onChange={(e) => {
                          const newStages = [...planForm.stages];
                          newStages[idx].stageName = e.target.value;
                          setPlanForm({ stages: newStages });
                        }}
                      />
                    </div>
                    <div>
                      <label style={{ fontSize: '11px', color: '#64748b' }}>Grant %</label>
                      <input
                        type="number"
                        className="form-input"
                        value={stage.percentageOfGrant}
                        onChange={(e) => {
                          const newStages = [...planForm.stages];
                          newStages[idx].percentageOfGrant = parseInt(e.target.value) || 0;
                          setPlanForm({ stages: newStages });
                        }}
                      />
                    </div>
                    <div>
                      <label style={{ fontSize: '11px', color: '#64748b' }}>Milestone Trigger</label>
                      <input
                        type="text"
                        className="form-input"
                        value={stage.triggerMilestone}
                        onChange={(e) => {
                          const newStages = [...planForm.stages];
                          newStages[idx].triggerMilestone = e.target.value;
                          setPlanForm({ stages: newStages });
                        }}
                      />
                    </div>
                    <div>
                      <label style={{ fontSize: '11px', color: '#64748b' }}>Offset Days</label>
                      <input
                        type="number"
                        className="form-input"
                        value={stage.dueDateOffsetDays}
                        onChange={(e) => {
                          const newStages = [...planForm.stages];
                          newStages[idx].dueDateOffsetDays = parseInt(e.target.value) || 0;
                          setPlanForm({ stages: newStages });
                        }}
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <button
              type="submit"
              disabled={createPlanMutation.isPending}
              className="btn btn-primary btn-md"
              style={{ width: '100%', justifyContent: 'center' }}
            >
              {createPlanMutation.isPending ? 'Saving Plan...' : 'Save 3-Stage Disbursement Plan →'}
            </button>
          </form>
        )}
      </Modal>

      {/* 6. Application Detail & Eligibility Recalculation Modal */}
      <Modal
        isOpen={appDetailModalOpen}
        onClose={() => setAppDetailModalOpen(false)}
        title={`Application Dossier #${applicationDetail?.id || selectedAppId}`}
        subtitle={`Applicant: ${applicationDetail?.beneficiary?.fullName || 'Citizen'} • Scheme: ${applicationDetail?.scheme?.schemeName || applicationDetail?.scheme?.name}`}
      >
        {appDetailLoading ? (
          <p>Loading dossier...</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="modal-form-grid-2" style={{ gap: '14px', backgroundColor: '#f8fafc', padding: '16px', borderRadius: '8px' }}>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Beneficiary Name</div>
                <div style={{ fontSize: '15px', fontWeight: 700 }}>{applicationDetail?.beneficiary?.fullName}</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>National ID (Aadhaar)</div>
                <div style={{ fontSize: '14px', fontWeight: 600 }}>•••• •••• {applicationDetail?.beneficiary?.nationalIdNumber?.slice(-4) || '1234'}</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Citizen Category</div>
                <div><span className="badge badge-primary">{applicationDetail?.beneficiary?.category}</span></div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Annual Income</div>
                <div style={{ fontSize: '14px', fontWeight: 700 }}>{formatINRFull(applicationDetail?.beneficiary?.annualIncome || 0)}</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Requested Subsidy Amount</div>
                <div style={{ fontSize: '16px', fontWeight: 800, color: '#059669' }}>{formatINRFull(applicationDetail?.requestedAmount)}</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Current Application Status</div>
                <div><StatusBadge status={applicationDetail?.status} /></div>
              </div>
            </div>

            {applicationDetail?.remarks && (
              <div style={{ padding: '12px', border: '1px solid #e2e8f0', borderRadius: '6px' }}>
                <div style={{ fontSize: '12px', color: '#64748b', fontWeight: 600 }}>Officer Remarks:</div>
                <div style={{ fontSize: '13px', marginTop: '4px' }}>{applicationDetail.remarks}</div>
              </div>
            )}

            {/* Audit Trail & Decision History for this Application */}
            <div style={{ marginTop: '8px', borderTop: '1px solid #e2e8f0', paddingTop: '14px' }}>
              <h4 style={{ fontSize: '14px', fontWeight: 700, marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <History size={16} color="#475569" />
                <span>Decision History & Audit Trail ({appAuditLogs.length})</span>
              </h4>
              {appAuditLoading ? (
                <p style={{ fontSize: '12px', color: '#64748b' }}>Loading decision timeline...</p>
              ) : appAuditLogs.length === 0 ? (
                <p style={{ fontSize: '12px', color: '#64748b' }}>No audit entries recorded for this application yet.</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '220px', overflowY: 'auto' }}>
                  {appAuditLogs.map((log) => (
                    <div key={log.id} style={{ padding: '8px 12px', backgroundColor: '#f8fafc', borderRadius: '6px', border: '1px solid #e2e8f0', fontSize: '12px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                        <span className={`badge ${
                          log.action?.includes('APPROV') || log.action?.includes('VERIF') || log.action?.includes('DISPATCH') || log.action?.includes('COMPLET')
                            ? 'badge-success'
                            : log.action?.includes('REJECT')
                            ? 'badge-danger'
                            : 'badge-primary'
                        }`} style={{ fontSize: '11px' }}>
                          {log.action}
                        </span>
                        <span style={{ color: '#64748b' }}>{formatDateTime(log.timestamp)}</span>
                      </div>
                      <div style={{ color: '#334155', fontWeight: 500 }}>{log.details}</div>
                      <div style={{ color: '#64748b', fontSize: '11px', marginTop: '2px' }}>
                        Actor: <strong>{log.actorName || 'System'}</strong> ({log.actorRole || 'SYSTEM'})
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid #e2e8f0', paddingTop: '14px' }}>
              <span style={{ fontSize: '12px', color: '#64748b' }}>
                Admin Rule: Recalculate automatic eligibility based on updated slabs or income.
              </span>
              <button
                onClick={() => recalculateEligibilityMutation.mutate(selectedAppId)}
                disabled={recalculateEligibilityMutation.isPending}
                className="btn btn-warning btn-sm"
              >
                <RefreshCw size={13} />
                <span>{recalculateEligibilityMutation.isPending ? 'Calculating...' : 'Recalculate Eligibility'}</span>
              </button>
            </div>
          </div>
        )}
      </Modal>

      {/* 7. Beneficiary Profile Modal */}
      <Modal
        isOpen={beneficiaryModalOpen}
        onClose={() => setBeneficiaryModalOpen(false)}
        title={`Citizen Profile: ${beneficiaryDetail?.fullName || 'Beneficiary'}`}
        subtitle={`DBT ID #${beneficiaryDetail?.id} • Region: ${beneficiaryDetail?.region}`}
      >
        {beneficiaryLoading ? (
          <p>Loading profile...</p>
        ) : (
          <div className="modal-form-grid-2" style={{ padding: '10px 0' }}>
            <div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Full Legal Name</div>
              <div style={{ fontSize: '16px', fontWeight: 700 }}>{beneficiaryDetail?.fullName}</div>
            </div>
            <div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Aadhaar Number</div>
              <div style={{ fontSize: '15px', fontWeight: 600 }}><code>{beneficiaryDetail?.nationalIdNumber}</code></div>
            </div>
            <div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Category</div>
              <div><span className="badge badge-primary">{beneficiaryDetail?.category}</span></div>
            </div>
            <div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Annual Income</div>
              <div style={{ fontSize: '15px', fontWeight: 700, color: '#059669' }}>{formatINRFull(beneficiaryDetail?.annualIncome || 0)}</div>
            </div>
            <div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>State / Region</div>
              <div style={{ fontSize: '14px', fontWeight: 600 }}>{beneficiaryDetail?.region}</div>
            </div>
            <div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Contact Phone</div>
              <div style={{ fontSize: '14px' }}>{beneficiaryDetail?.phoneNumber || '—'}</div>
            </div>
            <div style={{ gridColumn: 'span 2' }}>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Permanent Address</div>
              <div style={{ fontSize: '14px', marginTop: '2px' }}>{beneficiaryDetail?.address || '—'}</div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
