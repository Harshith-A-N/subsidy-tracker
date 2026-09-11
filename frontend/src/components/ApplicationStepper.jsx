import React from 'react';
import { APP_STATUS_INFO } from '../utils/constants';
import { Check, Clock, X, AlertCircle } from 'lucide-react';

export default function ApplicationStepper({ status }) {
  const currentInfo = APP_STATUS_INFO[status] || { stage: 0 };
  const currentStage = currentInfo.stage || 0;
  const isRejected = !!currentInfo.rejected;

  const STAGES = [
    { label: 'Submitted', stageNum: 1 },
    { label: 'Field Verification', stageNum: 2 },
    { label: 'District Approval', stageNum: 3 },
    { label: 'Finance Review', stageNum: 4 },
    { label: 'DBT Disbursed', stageNum: 5 },
  ];

  const n = STAGES.length;
  // For n items with flex: 1, center of item 0 is at (100% / (2 * n))
  // For n = 5: offset is 10%, span is 80% (from 10% to 90%)
  const offsetPct = 100 / (2 * n);
  const spanPct = 100 - 2 * offsetPct;
  const progressPct = Math.min(
    spanPct,
    Math.max(0, ((currentStage - 1) / (n - 1)) * spanPct)
  );

  return (
    <div style={{ padding: '12px 0', width: '100%', overflowX: 'auto', WebkitOverflowScrolling: 'touch' }}>
      <div className="stepper-container" style={{ minWidth: '380px' }}>
        {/* Inactive track line: strictly between circle 1 center and circle 5 center */}
        <div
          className="stepper-line"
          style={{
            position: 'absolute',
            top: '18px',
            left: `${offsetPct}%`,
            width: `${spanPct}%`,
            height: '3px',
            backgroundColor: 'var(--border-main)',
            zIndex: 1,
          }}
        />

        {/* Active progress line: terminates inside current stage circle */}
        <div
          className="stepper-progress"
          style={{
            position: 'absolute',
            top: '18px',
            left: `${offsetPct}%`,
            width: `${progressPct}%`,
            height: '3px',
            backgroundColor: isRejected ? 'var(--danger)' : 'var(--primary)',
            zIndex: 2,
            transition: 'width 0.3s ease',
          }}
        />

        {STAGES.map((s) => {
          const isCompleted = currentStage > s.stageNum;
          const isActive = currentStage === s.stageNum;
          const isFailed = isActive && isRejected;

          return (
            <div
              key={s.stageNum}
              className={`step-item ${isCompleted ? 'completed' : ''} ${isActive ? 'active' : ''}`}
              style={{ flex: 1, position: 'relative', zIndex: 3 }}
            >
              <div
                className="step-circle"
                style={{
                  position: 'relative',
                  zIndex: 4,
                  ...(isFailed
                    ? {
                        backgroundColor: 'var(--danger)',
                        borderColor: 'var(--danger)',
                        color: '#ffffff',
                      }
                    : {}),
                }}
              >
                {isCompleted ? (
                  <Check size={16} />
                ) : isFailed ? (
                  <X size={16} />
                ) : isActive ? (
                  <Clock size={16} />
                ) : (
                  s.stageNum
                )}
              </div>
              <div
                className="step-label"
                style={{
                  color: isFailed ? 'var(--danger-dark)' : undefined,
                }}
              >
                {s.label}
              </div>
            </div>
          );
        })}
      </div>

      {isRejected && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            backgroundColor: 'var(--danger-light)',
            color: 'var(--danger-dark)',
            padding: '8px 14px',
            borderRadius: '6px',
            fontSize: '13px',
            marginTop: '12px',
          }}
        >
          <AlertCircle size={16} />
          <span>This application was rejected at the current review stage.</span>
        </div>
      )}
    </div>
  );
}
