import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  Activity,
  AlertTriangle,
  BarChart3,
  BookOpen,
  CheckCircle2,
  Database,
  FileText,
  GraduationCap,
  History,
  Loader2,
  Play,
  RefreshCw,
  Search,
  ShieldCheck,
  Upload
} from 'lucide-react';
import './styles.css';

const emptyJob = {
  jobId: null,
  status: 'IDLE',
  validationPassed: null,
  validationOutput: null,
  errorMessage: null
};

function App() {
  const [health, setHealth] = useState('checking');
  const [actorId, setActorId] = useState('learner_001');
  const [session, setSession] = useState(null);
  const [domains, setDomains] = useState([]);
  const [documents, setDocuments] = useState([]);
  const [selectedDocId, setSelectedDocId] = useState('');
  const [selectedDomainIds, setSelectedDomainIds] = useState([4]);
  const [file, setFile] = useState(null);
  const [replaceFile, setReplaceFile] = useState(null);
  const [uploadResult, setUploadResult] = useState(null);
  const [job, setJob] = useState(emptyJob);
  const [modules, setModules] = useState([]);
  const [feedback, setFeedback] = useState([]);
  const [commentText, setCommentText] = useState('');
  const [changeRequestText, setChangeRequestText] = useState('');
  const [ragQuery, setRagQuery] = useState('What should compliance officers confirm before submission?');
  const [ragResult, setRagResult] = useState(null);
  const [learnerId, setLearnerId] = useState('learner_001');
  const [attemptScore, setAttemptScore] = useState('0.85');
  const [attemptResult, setAttemptResult] = useState(null);
  const [dueReviews, setDueReviews] = useState([]);
  const [auditEvents, setAuditEvents] = useState([]);
  const [incidents, setIncidents] = useState([]);
  const [busy, setBusy] = useState('');
  const [notice, setNotice] = useState('');

  const selectedDoc = useMemo(
    () => documents.find((document) => document.docId === selectedDocId),
    [documents, selectedDocId]
  );
  const isAdmin = session?.authorities?.includes('ROLE_ADMIN') || actorId === 'learner_001';

  useEffect(() => {
    refreshAll();
    setLearnerId(actorId);
  }, [actorId]);

  useEffect(() => {
    if (!selectedDocId) {
      setModules([]);
      setFeedback([]);
      return;
    }
    loadModules(selectedDocId);
    loadFeedback(selectedDocId);
  }, [selectedDocId]);

  useEffect(() => {
    if (!job.jobId || ['COMPLETED', 'FAILED'].includes(job.status)) {
      return undefined;
    }
    const id = window.setInterval(() => refreshJob(job.jobId, true), 1200);
    return () => window.clearInterval(id);
  }, [job.jobId, job.status]);

  async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set('X-Dev-User', actorId);
    const response = await fetch(path, { ...options, headers });
    if (!response.ok) {
      const text = await response.text();
      throw new Error(`${response.status} ${response.statusText}${text ? `: ${text}` : ''}`);
    }
    if (response.status === 204) {
      return null;
    }
    return response.json();
  }

  async function refreshAll() {
    setBusy('refresh');
    try {
      const [healthResult, sessionResult, domainsResult, documentsResult, auditResult, incidentResult] = await Promise.all([
        api('/actuator/health'),
        api('/api/v1/session').catch(() => null),
        api('/api/v1/domains'),
        api('/api/v1/documents?size=20&sort=createdAt,desc').catch(() => ({ content: [] })),
        api('/api/v1/admin/audit-events?size=12&sort=createdAt,desc').catch(() => ({ content: [] })),
        api('/api/v1/admin/incidents?size=12&sort=createdAt,desc').catch(() => ({ content: [] }))
      ]);
      setHealth(healthResult.status || 'unknown');
      setSession(sessionResult);
      setDomains(Array.isArray(domainsResult) ? domainsResult : []);
      const loadedDocuments = documentsResult.content || [];
      setDocuments(loadedDocuments);
      if (loadedDocuments.length > 0 && !loadedDocuments.some((document) => document.docId === selectedDocId)) {
        setSelectedDocId(loadedDocuments[0].docId);
      } else if (loadedDocuments.length === 0) {
        setSelectedDocId('');
      }
      setAuditEvents(auditResult.content || []);
      setIncidents(incidentResult.content || []);
    } catch (error) {
      setHealth('down');
      setNotice(error.message);
    } finally {
      setBusy('');
    }
  }

  async function handleUpload(event) {
    event.preventDefault();
    if (!file) {
      setNotice('Choose a document first.');
      return;
    }
    setBusy('upload');
    try {
      const form = new FormData();
      form.append('file', file);
      selectedDomainIds.forEach((domainId) => form.append('domain_ids', domainId));
      const result = await api('/api/v1/documents/upload', {
        method: 'POST',
        body: form
      });
      setUploadResult(result);
      setSelectedDocId(result.docId);
      setNotice('Document uploaded.');
      await refreshAll();
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy('');
    }
  }

  async function reprocessDocument() {
    if (!selectedDocId) {
      setNotice('Upload or select a document first.');
      return;
    }
    setBusy('reprocess');
    try {
      const result = await api(`/api/v1/documents/${selectedDocId}/reprocess`, { method: 'POST' });
      setJob(result);
      setNotice('Pipeline queued.');
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy('');
    }
  }

  async function replaceSource(event) {
    event.preventDefault();
    if (!selectedDocId || !replaceFile) {
      setNotice('Select a document and replacement file first.');
      return;
    }
    setBusy('replace');
    try {
      const form = new FormData();
      form.append('file', replaceFile);
      selectedDomainIds.forEach((domainId) => form.append('domain_ids', domainId));
      const result = await api(`/api/v1/documents/${selectedDocId}/source`, {
        method: 'PUT',
        body: form
      });
      setUploadResult(result);
      setJob(emptyJob);
      setNotice('Source replaced. Run reprocess to regenerate modules and embeddings.');
      await refreshAll();
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy('');
    }
  }

  async function refreshJob(jobId, quiet = false) {
    if (!quiet) {
      setBusy('job');
    }
    try {
      const result = await api(`/api/v1/jobs/${jobId}`);
      setJob(result);
      if (result.status === 'COMPLETED') {
        await Promise.all([loadModules(selectedDocId), refreshGovernance()]);
      }
      if (result.status === 'FAILED') {
        await refreshGovernance();
      }
    } catch (error) {
      setNotice(error.message);
    } finally {
      if (!quiet) {
        setBusy('');
      }
    }
  }

  async function loadModules(docId) {
    try {
      const result = await api(`/api/v1/documents/${docId}/modules`);
      setModules(result);
    } catch {
      setModules([]);
    }
  }

  async function loadFeedback(docId) {
    try {
      const result = await api(`/api/v1/documents/${docId}/feedback`);
      setFeedback(result);
    } catch {
      setFeedback([]);
    }
  }

  async function addFeedback(type, comment = '') {
    if (!selectedDocId) {
      setNotice('Select a document first.');
      return;
    }
    setBusy('feedback');
    try {
      await api(`/api/v1/documents/${selectedDocId}/feedback`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          feedback_type: type,
          comment
        })
      });
      setCommentText('');
      setChangeRequestText('');
      await Promise.all([loadFeedback(selectedDocId), refreshGovernance()]);
      setNotice(type === 'READ_MARK' ? 'Document marked as read.' : 'Feedback saved.');
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy('');
    }
  }

  async function runRagQuery(event) {
    event.preventDefault();
    setBusy('rag');
    try {
      const result = await api('/api/v1/rag/query', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: ragQuery,
          domain_ids: selectedDomainIds,
          top_k: 3
        })
      });
      setRagResult(result);
      await refreshGovernance();
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy('');
    }
  }

  async function recordAttempt(moduleId) {
    setBusy('attempt');
    try {
      const result = await api(`/api/v1/modules/${moduleId}/attempts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          learner_id: learnerId,
          score: Number(attemptScore),
          interaction_seconds: 120
        })
      });
      setAttemptResult(result);
      await loadDueReviews();
      await refreshGovernance();
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy('');
    }
  }

  async function loadDueReviews() {
    try {
      const result = await api(`/api/v1/learners/${encodeURIComponent(learnerId)}/reviews/due`);
      setDueReviews(result);
    } catch (error) {
      setNotice(error.message);
    }
  }

  async function refreshGovernance() {
    const [auditResult, incidentResult] = await Promise.all([
      api('/api/v1/admin/audit-events?size=12&sort=createdAt,desc').catch(() => ({ content: [] })),
      api('/api/v1/admin/incidents?size=12&sort=createdAt,desc').catch(() => ({ content: [] }))
    ]);
    setAuditEvents(auditResult.content || []);
    setIncidents(incidentResult.content || []);
  }

  function toggleDomain(domainId) {
    setSelectedDomainIds((current) => {
      if (current.includes(domainId)) {
        return current.length === 1 ? current : current.filter((id) => id !== domainId);
      }
      return [...current, domainId];
    });
  }

  const isBusy = (key) => busy === key;

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">KGI Knowledge Shredder</p>
          <h1>Training Intelligence Console</h1>
        </div>
        <div className="headerActions">
          <label className="roleSwitch">
            Active role
            <select value={actorId} onChange={(event) => setActorId(event.target.value)}>
              <option value="learner_001">learner_001 · admin</option>
              <option value="learner_002">learner_002 · user</option>
            </select>
          </label>
          <div className="statusPill">
            <span className={health === 'UP' ? 'dot up' : 'dot'} />
            API {health}
          </div>
        </div>
      </header>

      {notice && (
        <section className="notice">
          <AlertTriangle size={18} />
          <span>{notice}</span>
          <button type="button" onClick={() => setNotice('')} aria-label="Dismiss notice">x</button>
        </section>
      )}

      <section className="metricsGrid">
        <Metric icon={Database} label="Documents" value={documents.length} />
        <Metric icon={BookOpen} label="Modules" value={modules.length} />
        <Metric icon={History} label="Audit Events" value={auditEvents.length} />
        <Metric icon={AlertTriangle} label="Open Incidents" value={incidents.filter((item) => !item.resolved).length} />
      </section>

      <section className="workspace">
        <div className="panel wide">
          <PanelTitle icon={Upload} title="Ingestion" action={
            <button type="button" className="iconButton" onClick={refreshAll} title="Refresh">
              <RefreshCw size={17} className={isBusy('refresh') ? 'spin' : ''} />
            </button>
          } />
          {!isAdmin && <p className="roleNote">Read-only users can review files, leave comments, mark read, and request changes.</p>}
          <form className="uploadForm" onSubmit={handleUpload}>
            <label className="fileDrop">
              <FileText size={24} />
              <span>{file ? file.name : 'Choose TXT, PDF, DOCX, or source file'}</span>
              <input disabled={!isAdmin} type="file" onChange={(event) => setFile(event.target.files?.[0] || null)} />
            </label>
            <div className="domainList">
              {domains.map((domain) => (
                <button
                  type="button"
                  key={domain.domainId}
                  className={selectedDomainIds.includes(domain.domainId) ? 'chip selected' : 'chip'}
                  onClick={() => toggleDomain(domain.domainId)}
                >
                  {domain.domainName}
                </button>
              ))}
            </div>
            <button className="primaryButton" type="submit" disabled={!isAdmin || isBusy('upload')}>
              {isBusy('upload') ? <Loader2 size={18} className="spin" /> : <Upload size={18} />}
              Upload
            </button>
          </form>
          {uploadResult && (
            <div className="previewBox">
              <strong>Preview</strong>
              <p>{uploadResult.previewText}</p>
            </div>
          )}
        </div>

        <div className="panel">
          <PanelTitle icon={FileText} title="Documents" />
          <div className="selectGroup">
            <label>Current document</label>
            <select value={selectedDocId} onChange={(event) => setSelectedDocId(event.target.value)}>
              <option value="">No document selected</option>
              {documents.map((document) => (
                <option key={document.docId} value={document.docId}>{document.originalFilename}</option>
              ))}
            </select>
          </div>
          {selectedDoc && (
            <dl className="detailList">
              <div><dt>Document ID</dt><dd>{selectedDoc.docId}</dd></div>
              <div><dt>Version</dt><dd>{selectedDoc.currentVersionId}</dd></div>
            </dl>
          )}
          {isAdmin && (
            <form className="replaceForm" onSubmit={replaceSource}>
              <label className="fileDrop compactDrop">
                <FileText size={20} />
                <span>{replaceFile ? replaceFile.name : 'Replacement source for polluted or updated material'}</span>
                <input type="file" onChange={(event) => setReplaceFile(event.target.files?.[0] || null)} />
              </label>
              <button className="secondaryButton" type="submit" disabled={!selectedDocId || !replaceFile || isBusy('replace')}>
                {isBusy('replace') ? <Loader2 size={18} className="spin" /> : <RefreshCw size={18} />}
                Replace Source
              </button>
            </form>
          )}
          <button className="secondaryButton" type="button" onClick={reprocessDocument} disabled={!isAdmin || !selectedDocId || isBusy('reprocess')}>
            {isBusy('reprocess') ? <Loader2 size={18} className="spin" /> : <Play size={18} />}
            Reprocess
          </button>
        </div>

        <div className="panel">
          <PanelTitle icon={History} title="Review Feedback" />
          <button className="secondaryButton" type="button" onClick={() => addFeedback('READ_MARK')} disabled={!selectedDocId || isBusy('feedback')}>
            <CheckCircle2 size={18} />
            Mark Read
          </button>
          <div className="feedbackForms">
            <label>
              Comment
              <textarea value={commentText} onChange={(event) => setCommentText(event.target.value)} rows={3} />
            </label>
            <button className="textButton" type="button" onClick={() => addFeedback('COMMENT', commentText)} disabled={!selectedDocId || !commentText.trim()}>
              Add Comment
            </button>
            <label>
              Change request
              <textarea value={changeRequestText} onChange={(event) => setChangeRequestText(event.target.value)} rows={3} />
            </label>
            <button className="textButton" type="button" onClick={() => addFeedback('CHANGE_REQUEST', changeRequestText)} disabled={!selectedDocId || !changeRequestText.trim()}>
              Request Admin Update
            </button>
          </div>
          <div className="listRows">
            {feedback.length === 0 && <EmptyState text="No review feedback yet." />}
            {feedback.map((item) => (
              <div className="rowItem" key={item.feedbackId}>
                <span>{item.feedbackType} · {item.actorId}</span>
                <small>{item.comment || item.status} · {new Date(item.createdAt).toLocaleString()}</small>
              </div>
            ))}
          </div>
        </div>

        <div className="panel">
          <PanelTitle icon={Activity} title="Pipeline" />
          <div className="jobState">
            <span className={`stateBadge ${job.status.toLowerCase()}`}>{job.status}</span>
            {job.validationPassed !== null && (
              <span className="validationBadge">
                {job.validationPassed ? <CheckCircle2 size={16} /> : <AlertTriangle size={16} />}
                Checkpoint {String(job.validationPassed)}
              </span>
            )}
          </div>
          <dl className="detailList">
            <div><dt>Job ID</dt><dd>{job.jobId || 'Not queued'}</dd></div>
            <div><dt>Error</dt><dd>{job.errorMessage || 'None'}</dd></div>
          </dl>
          {job.validationOutput && <pre className="jsonBlock">{job.validationOutput}</pre>}
        </div>

        <div className="panel wide">
          <PanelTitle icon={BookOpen} title="Micro Modules" />
          <div className="moduleGrid">
            {modules.length === 0 && <EmptyState text="No modules yet. Upload a document and run the pipeline." />}
            {modules.map((module) => (
              <article className="moduleCard" key={module.moduleId}>
                <div className="moduleHeader">
                  <span className="sequence">#{module.sequenceOrder}</span>
                  <span className={module.validated ? 'validFlag' : 'validFlag pending'}>
                    {module.validated ? 'Validated' : 'Pending'}
                  </span>
                </div>
                <h2>{module.title}</h2>
                <p>{module.content}</p>
                <strong>{module.keyTakeaway}</strong>
                <div className="moduleMeta">
                  <span>{module.readingTimeMinutes} min</span>
                  <span>Score {module.validationScore || 'n/a'}</span>
                </div>
                <button className="textButton" type="button" onClick={() => recordAttempt(module.moduleId)}>
                  <GraduationCap size={17} />
                  Record Attempt
                </button>
              </article>
            ))}
          </div>
        </div>

        <div className="panel">
          <PanelTitle icon={Search} title="RAG Query" />
          <form className="stackForm" onSubmit={runRagQuery}>
            <textarea value={ragQuery} onChange={(event) => setRagQuery(event.target.value)} rows={4} />
            <button className="primaryButton" type="submit" disabled={isBusy('rag')}>
              {isBusy('rag') ? <Loader2 size={18} className="spin" /> : <Search size={18} />}
              Query
            </button>
          </form>
          {ragResult && (
            <div className="answerBox">
              <strong>Answer</strong>
              <p>{ragResult.answer}</p>
              <div className="sourceList">
                {ragResult.sources.map((source) => (
                  <div key={source.chunkId}>
                    <span>{source.similarityScore.toFixed(3)}</span>
                    <p>{source.excerpt}</p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="panel">
          <PanelTitle icon={GraduationCap} title="Learning" action={
            <button type="button" className="iconButton" onClick={loadDueReviews} title="Refresh reviews">
              <RefreshCw size={17} />
            </button>
          } />
          <div className="splitInputs">
            <label>
              Learner
              <input value={learnerId} onChange={(event) => setLearnerId(event.target.value)} />
            </label>
            <label>
              Score
              <input type="number" min="0" max="1" step="0.05" value={attemptScore} onChange={(event) => setAttemptScore(event.target.value)} />
            </label>
          </div>
          {attemptResult && (
            <div className="previewBox compact">
              Next review: {new Date(attemptResult.nextReviewAt).toLocaleString()}
            </div>
          )}
          <div className="listRows">
            {dueReviews.length === 0 && <EmptyState text="No reviews due." />}
            {dueReviews.map((review) => (
              <div className="rowItem" key={review.stateId}>
                <span>{review.title}</span>
                <small>{new Date(review.nextReviewAt).toLocaleString()}</small>
              </div>
            ))}
          </div>
        </div>

        <div className="panel">
          <PanelTitle icon={ShieldCheck} title="Governance" />
          <div className="tabs">
            <span>Audit</span>
            <span>Incidents</span>
          </div>
          <div className="governanceGrid">
            <div className="listRows">
              {auditEvents.length === 0 && <EmptyState text="No audit events yet." />}
              {auditEvents.map((event) => (
                <div className="rowItem" key={event.eventId}>
                  <span>{event.eventType}</span>
                  <small>{event.entityId}</small>
                </div>
              ))}
            </div>
            <div className="listRows">
              {incidents.length === 0 && <EmptyState text="No incidents." />}
              {incidents.map((incident) => (
                <div className="rowItem incident" key={incident.incidentId}>
                  <span>{incident.incidentType}</span>
                  <small>{incident.severity} · {incident.resolved ? 'Resolved' : 'Open'}</small>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="panel">
          <PanelTitle icon={BarChart3} title="Verification Targets" />
          <ul className="checkList">
            <li className={job.status === 'COMPLETED' ? 'done' : ''}>Pipeline reaches COMPLETED</li>
            <li className={modules.some((module) => module.validated) ? 'done' : ''}>Validated micro modules exist</li>
            <li className={ragResult?.sources?.length ? 'done' : ''}>RAG returns tenant-scoped sources</li>
            <li className={Boolean(attemptResult) ? 'done' : ''}>Learner attempt updates memory</li>
            <li className={auditEvents.length > 0 ? 'done' : ''}>Audit trail records activity</li>
          </ul>
        </div>
      </section>
    </main>
  );
}

function PanelTitle({ icon: Icon, title, action }) {
  return (
    <div className="panelTitle">
      <div>
        <Icon size={19} />
        <h2>{title}</h2>
      </div>
      {action}
    </div>
  );
}

function Metric({ icon: Icon, label, value }) {
  return (
    <div className="metric">
      <Icon size={20} />
      <div>
        <strong>{value}</strong>
        <span>{label}</span>
      </div>
    </div>
  );
}

function EmptyState({ text }) {
  return <p className="emptyState">{text}</p>;
}

createRoot(document.getElementById('root')).render(<App />);
