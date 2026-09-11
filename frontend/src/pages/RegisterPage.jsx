import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { BENEFICIARY_CATEGORIES, INDIAN_STATES } from '../utils/constants';
import { User, Shield, ArrowLeft } from 'lucide-react';

export default function RegisterPage() {
  const [tab, setTab] = useState('citizen'); // 'citizen' | 'officer'
  const [loading, setLoading] = useState(false);
  const { registerCitizen, submitOfficerRequest } = useAuth();
  const navigate = useNavigate();

  // Citizen Form State
  const [citizenData, setCitizenData] = useState({
    fullName: '',
    email: '',
    password: '',
    phoneNumber: '',
    nationalIdNumber: '',
    address: '',
    category: 'FARMER',
    region: 'Maharashtra',
    annualIncome: '',
  });

  // Officer Request Form State
  const [officerData, setOfficerData] = useState({
    fullName: '',
    email: '',
    password: '',
    phoneNumber: '',
    requestedRole: 'FIELD_OFFICER',
    region: 'Maharashtra',
  });

  const handleCitizenSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    // Backend RegisterRequestDto expects: fullName, email, password
    const payload = {
      fullName: citizenData.fullName,
      email: citizenData.email,
      password: citizenData.password,
    };
    // Save remaining profile details in session to auto-complete profile upon login
    try {
      sessionStorage.setItem('pending_reg_profile', JSON.stringify(citizenData));
    } catch { }

    const res = await registerCitizen(payload);
    setLoading(false);
    if (res.success) {
      navigate('/login');
    }
  };

  const handleOfficerSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    const payload = {
      fullName: officerData.fullName,
      email: officerData.email,
      password: officerData.password,
      phone: officerData.phoneNumber,
      requestedRole: officerData.requestedRole,
      region: officerData.region,
    };
    const res = await submitOfficerRequest(payload);
    setLoading(false);
    if (res.success) {
      navigate('/login');
    }
  };

  return (
    <div style={{
      minHeight: 'calc(100vh - 120px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: 'clamp(20px, 4vw, 40px) clamp(12px, 3vw, 20px)',
      backgroundColor: 'var(--bg-main)',
    }}>
      <div className="card fade-in" style={{ width: '100%', maxWidth: '640px', padding: 'clamp(18px, 3.5vw, 32px)' }}>
        <div style={{ marginBottom: '16px' }}>
          <Link
            to="/"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '13px',
              fontWeight: 600,
              color: '#64748b',
              textDecoration: 'none',
              transition: 'color 0.2s',
            }}
            onMouseOver={(e) => (e.currentTarget.style.color = '#1d4ed8')}
            onMouseOut={(e) => (e.currentTarget.style.color = '#64748b')}
          >
            <ArrowLeft size={16} />
            <span>Back to Home</span>
          </Link>
        </div>

        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <h2 style={{ fontSize: 'clamp(20px, 2.5vw, 24px)', fontWeight: 800, color: '#0f172a' }}>
            GovGrant Portal Registration
          </h2>
          <p style={{ fontSize: '13px', color: '#64748b', marginTop: '4px' }}>
            Choose your account type to proceed with enrollment
          </p>
        </div>

        {/* Tab Toggle */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))',
          gap: '8px',
          padding: '4px',
          backgroundColor: '#f1f5f9',
          borderRadius: '10px',
          marginBottom: '24px',
        }}>
          <button
            type="button"
            onClick={() => setTab('citizen')}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              padding: '10px',
              borderRadius: '8px',
              border: 'none',
              cursor: 'pointer',
              fontWeight: 700,
              fontSize: '13px',
              backgroundColor: tab === 'citizen' ? '#ffffff' : 'transparent',
              color: tab === 'citizen' ? '#1d4ed8' : '#64748b',
              boxShadow: tab === 'citizen' ? '0 2px 4px rgba(0,0,0,0.06)' : 'none',
              transition: 'all 0.15s ease',
            }}
          >
            <User size={16} />
            <span>Citizen Beneficiary</span>
          </button>

          <button
            type="button"
            onClick={() => setTab('officer')}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              padding: '10px',
              borderRadius: '8px',
              border: 'none',
              cursor: 'pointer',
              fontWeight: 700,
              fontSize: '13px',
              backgroundColor: tab === 'officer' ? '#ffffff' : 'transparent',
              color: tab === 'officer' ? '#1d4ed8' : '#64748b',
              boxShadow: tab === 'officer' ? '0 2px 4px rgba(0,0,0,0.06)' : 'none',
              transition: 'all 0.15s ease',
            }}
          >
            <Shield size={16} />
            <span>Officer Registration</span>
          </button>
        </div>

        {/* Citizen Beneficiary Form */}
        {tab === 'citizen' ? (
          <form onSubmit={handleCitizenSubmit}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 240px), 1fr))', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label">Full Name <span className="req">*</span></label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Ramesh Kumar"
                  className="form-input"
                  value={citizenData.fullName}
                  onChange={(e) => setCitizenData({ ...citizenData, fullName: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Aadhaar (National ID) <span className="req">*</span></label>
                <input
                  type="text"
                  required
                  maxLength={12}
                  placeholder="12-digit Aadhaar No."
                  className="form-input"
                  value={citizenData.nationalIdNumber}
                  onChange={(e) => setCitizenData({ ...citizenData, nationalIdNumber: e.target.value })}
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label">Email Address <span className="req">*</span></label>
                <input
                  type="email"
                  required
                  placeholder="name@example.com"
                  className="form-input"
                  value={citizenData.email}
                  onChange={(e) => setCitizenData({ ...citizenData, email: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Mobile Number <span className="req">*</span></label>
                <input
                  type="tel"
                  required
                  maxLength={10}
                  placeholder="10-digit mobile"
                  className="form-input"
                  value={citizenData.phoneNumber}
                  onChange={(e) => setCitizenData({ ...citizenData, phoneNumber: e.target.value })}
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label">Target Category <span className="req">*</span></label>
                <select
                  className="form-select"
                  value={citizenData.category}
                  onChange={(e) => setCitizenData({ ...citizenData, category: e.target.value })}
                >
                  {BENEFICIARY_CATEGORIES.map((c) => (
                    <option key={c} value={c}>{c.replace('_', ' ')}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">State / Region <span className="req">*</span></label>
                <select
                  className="form-select"
                  value={citizenData.region}
                  onChange={(e) => setCitizenData({ ...citizenData, region: e.target.value })}
                >
                  {INDIAN_STATES.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label">Annual Income (₹) <span className="req">*</span></label>
                <input
                  type="number"
                  required
                  min="0"
                  placeholder="e.g. 150000"
                  className="form-input"
                  value={citizenData.annualIncome}
                  onChange={(e) => setCitizenData({ ...citizenData, annualIncome: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Create Password <span className="req">*</span></label>
                <input
                  type="password"
                  required
                  minLength={6}
                  placeholder="Min 6 characters"
                  className="form-input"
                  value={citizenData.password}
                  onChange={(e) => setCitizenData({ ...citizenData, password: e.target.value })}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Residential Address <span className="req">*</span></label>
              <input
                type="text"
                required
                placeholder="Village / Town, District, Pincode"
                className="form-input"
                value={citizenData.address}
                onChange={(e) => setCitizenData({ ...citizenData, address: e.target.value })}
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary btn-lg"
              style={{ width: '100%', justifyContent: 'center', marginTop: '12px' }}
            >
              {loading ? 'Creating Account...' : 'Register Citizen Account →'}
            </button>
          </form>
        ) : (
          /* Officer Registration Request Form */
          <form onSubmit={handleOfficerSubmit}>
            <div style={{
              backgroundColor: '#eff6ff',
              border: '1px solid #bfdbfe',
              padding: '12px 16px',
              borderRadius: '8px',
              fontSize: '13px',
              color: '#1e40af',
              marginBottom: '20px',
            }}>
              Officer accounts require verification by the System Administrator before activation.
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 240px), 1fr))', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label">Officer Name <span className="req">*</span></label>
                <input
                  type="text"
                  required
                  placeholder="Officer Name"
                  className="form-input"
                  value={officerData.fullName}
                  onChange={(e) => setOfficerData({ ...officerData, fullName: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Official Email <span className="req">*</span></label>
                <input
                  type="email"
                  required
                  placeholder="officer@nic.in / dept.gov.in"
                  className="form-input"
                  value={officerData.email}
                  onChange={(e) => setOfficerData({ ...officerData, email: e.target.value })}
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 240px), 1fr))', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label">Requested Role <span className="req">*</span></label>
                <select
                  className="form-select"
                  value={officerData.requestedRole}
                  onChange={(e) => setOfficerData({ ...officerData, requestedRole: e.target.value })}
                >
                  <option value="FIELD_OFFICER">Field Officer (Ground Verification)</option>
                  <option value="DISTRICT_OFFICER">District Officer (Sanctioning Authority)</option>
                  <option value="FINANCE_APPROVER">Finance Approver (Treasury Release)</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Assigned Region / State <span className="req">*</span></label>
                <select
                  className="form-select"
                  value={officerData.region}
                  onChange={(e) => setOfficerData({ ...officerData, region: e.target.value })}
                >
                  {INDIAN_STATES.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Contact Phone <span className="req">*</span></label>
              <input
                type="tel"
                required
                maxLength={10}
                placeholder="10-digit mobile"
                className="form-input"
                value={officerData.phoneNumber}
                onChange={(e) => setOfficerData({ ...officerData, phoneNumber: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Create Password <span className="req">*</span></label>
              <input
                type="password"
                required
                minLength={6}
                placeholder="Min 6 characters"
                className="form-input"
                value={officerData.password}
                onChange={(e) => setOfficerData({ ...officerData, password: e.target.value })}
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary btn-lg"
              style={{ width: '100%', justifyContent: 'center', marginTop: '12px' }}
            >
              {loading ? 'Submitting Request...' : 'Submit Officer Request for Approval →'}
            </button>
          </form>
        )}

        <div style={{ textAlign: 'center', marginTop: '20px', fontSize: '13px', color: '#64748b' }}>
          Already registered?{' '}
          <Link to="/login" style={{ fontWeight: 600, color: '#1d4ed8' }}>
            Sign In Here
          </Link>
        </div>
      </div>
    </div>
  );
}
