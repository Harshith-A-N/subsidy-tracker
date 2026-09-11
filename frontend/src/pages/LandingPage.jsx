import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../api/client';
import { formatINR } from '../utils/formatters';
import { Shield, CheckCircle2, ArrowRight, Landmark, Search } from 'lucide-react';

export default function LandingPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');

  // Fetch published schemes with React Query caching
  const { data: schemes = [], isLoading } = useQuery({
    queryKey: ['public-schemes'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/schemes');
      return res.success && Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 5 * 60 * 1000,
  });

  const filteredSchemes = schemes.filter((s) => {
    const matchesSearch = !searchTerm || (s.name || s.schemeName || '').toLowerCase().includes(searchTerm.toLowerCase()) || (s.description || '').toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = !selectedCategory || (s.targetCategory || '').toUpperCase() === selectedCategory.toUpperCase();
    return matchesSearch && matchesCategory;
  });

  return (
    <div className="fade-in">
      {/* Hero Section */}
      <section style={{
        background: 'linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #1d4ed8 100%)',
        color: '#ffffff',
        padding: 'clamp(32px, 5vw, 64px) clamp(14px, 3vw, 24px) clamp(48px, 6vw, 80px)',
        position: 'relative',
        overflow: 'hidden',
      }}>
        <div style={{ maxWidth: '1280px', margin: '0 auto' }}>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 360px), 1fr))',
            gap: 'clamp(24px, 3vw, 40px)',
            alignItems: 'center',
          }}>
            {/* Left Hero Content */}
            <div>
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
                backgroundColor: 'rgba(255, 255, 255, 0.12)',
                backdropFilter: 'blur(8px)',
                padding: '6px 14px',
                borderRadius: '999px',
                fontSize: 'clamp(11.5px, 1.3vw, 13px)',
                fontWeight: 600,
                marginBottom: '18px',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                maxWidth: '100%',
              }}>
                <Shield size={16} color="#60a5fa" style={{ minWidth: '16px' }} />
                <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>Official Government Subsidy & Grant Portal</span>
              </div>

              <h1 style={{
                fontSize: 'clamp(26px, 4vw, 44px)',
                fontWeight: 800,
                lineHeight: 1.15,
                marginBottom: '16px',
                color: '#ffffff',
                letterSpacing: '-0.02em',
              }}>
                Direct Benefit Transfer & Subsidies Made <span style={{ color: '#93c5fd' }}>Transparent & Instant</span>.
              </h1>

              <p style={{
                fontSize: 'clamp(14px, 1.5vw, 16px)',
                color: '#e2e8f0',
                lineHeight: 1.6,
                marginBottom: '28px',
                maxWidth: '560px',
              }}>
                Apply for central & state agricultural, MSME, and education grant schemes. Real-time eligibility evaluation, multi-tier officer sanctioning, and direct treasury disbursements.
              </p>

              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
                <Link to="/register" className="btn btn-primary btn-lg" style={{ backgroundColor: '#ffffff', color: '#1e3a8a', fontWeight: 700 }}>
                  <span>Apply for Subsidies</span>
                  <ArrowRight size={18} />
                </Link>
                <Link to="/login" className="btn btn-secondary btn-lg" style={{ backgroundColor: 'rgba(255, 255, 255, 0.1)', color: '#ffffff', borderColor: 'rgba(255, 255, 255, 0.3)' }}>
                  <span>Officer / Staff Login</span>
                </Link>
              </div>
            </div>

            {/* Quick Metrics Hero Card */}
            <div className="card" style={{
              backgroundColor: 'rgba(255, 255, 255, 0.98)',
              color: '#0f172a',
              padding: 'clamp(18px, 2.5vw, 28px)',
              borderRadius: '16px',
              boxShadow: '0 20px 40px -15px rgba(0, 0, 0, 0.3)',
              width: '100%',
            }}>
              <h3 style={{ fontSize: 'clamp(16px, 1.8vw, 18px)', fontWeight: 700, marginBottom: '18px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Landmark size={20} color="#1d4ed8" style={{ minWidth: '20px' }} />
                <span>Live Government Grant Statistics</span>
              </h3>

              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 140px), 1fr))',
                gap: '12px',
                marginBottom: '18px',
              }}>
                <div style={{ padding: '12px', backgroundColor: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                  <div style={{ fontSize: '11.5px', color: '#64748b', fontWeight: 600 }}>Active Schemes</div>
                  <div style={{ fontSize: 'clamp(20px, 2.5vw, 24px)', fontWeight: 800, color: '#1d4ed8', marginTop: '4px' }}>
                    {schemes.length || '12+'}
                  </div>
                </div>

                <div style={{ padding: '12px', backgroundColor: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                  <div style={{ fontSize: '11.5px', color: '#64748b', fontWeight: 600 }}>Total Funds Allocated</div>
                  <div style={{ fontSize: 'clamp(20px, 2.5vw, 24px)', fontWeight: 800, color: '#059669', marginTop: '4px' }}>
                    ₹ 850+ Cr
                  </div>
                </div>
              </div>

              <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: '14px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '12.5px', color: '#334155' }}>
                  <CheckCircle2 size={16} color="#059669" style={{ minWidth: '16px' }} />
                  <span>100% Aadhaar & Direct Bank Transfer (DBT)</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '12.5px', color: '#334155' }}>
                  <CheckCircle2 size={16} color="#059669" style={{ minWidth: '16px' }} />
                  <span>Geo-Tagged Field Officer Verification</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '12.5px', color: '#334155' }}>
                  <CheckCircle2 size={16} color="#059669" style={{ minWidth: '16px' }} />
                  <span>Zero Intermediary / End-to-End Audited</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Scheme Discovery & Catalog */}
      <section style={{ maxWidth: '1280px', margin: '-30px auto 60px', padding: '0 clamp(12px, 2vw, 24px)', position: 'relative', zIndex: 10 }}>
        <div className="card" style={{ padding: 'clamp(16px, 2.5vw, 24px)', marginBottom: '32px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '14px', marginBottom: '20px' }}>
            <div style={{ minWidth: '240px' }}>
              <h2 style={{ fontSize: 'clamp(18px, 2vw, 22px)', fontWeight: 800 }}>Explore Available Schemes & Grants</h2>
              <p style={{ fontSize: '13px', color: '#64748b', marginTop: '2px' }}>
                Browse government grants tailored for farmers, MSMEs, students, and artisans.
              </p>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap', width: '100%', maxWidth: '520px' }}>
              <div style={{ position: 'relative', flex: 1, minWidth: '200px' }}>
                <Search size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '12px' }} />
                <input
                  type="text"
                  placeholder="Search schemes..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="form-input"
                  style={{ paddingLeft: '36px', width: '100%' }}
                />
              </div>

              <select
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="form-select"
                style={{ width: 'auto', minWidth: '150px', flex: '0 1 auto' }}
              >
                <option value="">All Categories</option>
                <option value="FARMER">Farmers / Agriculture</option>
                <option value="MSME">MSMEs & Small Business</option>
                <option value="STUDENT">Students & Education</option>
                <option value="WOMEN_ENTREPRENEUR">Women Entrepreneurs</option>
                <option value="ARTISAN">Artisans & Crafts</option>
              </select>
            </div>
          </div>

          {/* Scheme Cards Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 300px), 1fr))', gap: '18px' }}>
            {filteredSchemes.map((scheme) => (
              <div key={scheme.id} className="card card-hover" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
                    <span className="badge badge-primary">
                      {scheme.targetCategory || 'GENERAL'}
                    </span>
                    <span style={{ fontSize: '11.5px', color: '#64748b', fontWeight: 600 }}>
                      Code: #{scheme.id}
                    </span>
                  </div>

                  <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '8px', color: '#0f172a', lineHeight: 1.3 }}>
                    {scheme.name || scheme.schemeName}
                  </h3>

                  <p style={{ fontSize: '12.5px', color: '#475569', lineHeight: 1.5, marginBottom: '16px' }}>
                    {scheme.description || 'Government assistance and grant support for eligible citizens.'}
                  </p>
                </div>

                <div style={{ borderTop: '1px solid #f1f5f9', paddingTop: '14px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px', flexWrap: 'wrap', gap: '8px' }}>
                    <div>
                      <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>Total Allocation</div>
                      <div style={{ fontSize: '15px', fontWeight: 800, color: '#059669' }}>
                        {formatINR(scheme.totalBudget || 10000000)}
                      </div>
                    </div>
                    {scheme.maxIncomeLimit && (
                      <div style={{ textAlign: 'right' }}>
                        <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>Max Income</div>
                        <div style={{ fontSize: '13.5px', fontWeight: 700, color: '#334155' }}>
                          {formatINR(scheme.maxIncomeLimit)}/yr
                        </div>
                      </div>
                    )}
                  </div>

                  <Link to="/register" className="btn btn-primary btn-sm" style={{ width: '100%', justifyContent: 'center' }}>
                    <span>Apply for this Scheme</span>
                    <ArrowRight size={14} />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
