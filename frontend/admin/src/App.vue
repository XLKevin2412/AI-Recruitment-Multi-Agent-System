<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">AI Recruitment Workspace</p>
        <h1>招聘多智能体工作台</h1>
      </div>
      <div class="health-group">
        <button class="icon-button" type="button" title="刷新服务状态" @click="loadHealth">
          <RefreshCw :size="18" />
        </button>
        <span class="status-pill" :class="healthClass">
          <span class="status-dot"></span>
          {{ healthLabel }}
        </span>
      </div>
    </header>

    <section class="workflow-grid">
      <aside class="workflow-panel">
        <div class="panel-heading">
          <BriefcaseBusiness :size="18" />
          <span>主流程</span>
        </div>
        <button
          v-for="step in steps"
          :key="step.key"
          class="step-button"
          :class="{ active: activeStep === step.key, done: stepDone(step.key) }"
          type="button"
          @click="activeStep = step.key"
        >
          <component :is="step.icon" :size="18" />
          <span>{{ step.label }}</span>
        </button>
      </aside>

      <section class="content-panel">
        <div v-if="message.text" class="message" :class="message.type">
          {{ message.text }}
        </div>

        <section v-show="activeStep === 'job'" class="section-layout">
          <div class="section-title">
            <h2>岗位信息</h2>
            <button class="secondary-button" type="button" @click="loadJobs">
              <ListRestart :size="16" />
              刷新岗位
            </button>
          </div>

          <div class="form-grid">
            <label>
              岗位名称
              <input v-model.trim="jobForm.title" placeholder="AI 应用工程师" />
            </label>
            <label>
              岗位类型
              <input v-model.trim="jobForm.jobType" placeholder="AI_APP_ENGINEER" />
            </label>
            <label>
              部门
              <input v-model.trim="jobForm.department" placeholder="AI 平台部" />
            </label>
            <label>
              级别
              <input v-model.trim="jobForm.level" placeholder="初级/中级" />
            </label>
            <label>
              必备技能
              <input v-model.trim="jobForm.requiredSkills" placeholder="Python, FastAPI, RAG, Milvus" />
            </label>
            <label>
              加分技能
              <input v-model.trim="jobForm.preferredSkills" placeholder="Docker, Redis, Vue" />
            </label>
            <label class="wide">
              经验要求
              <textarea v-model.trim="jobForm.experienceRequirement" rows="3"></textarea>
            </label>
            <label class="wide">
              岗位描述
              <textarea v-model.trim="jobForm.description" rows="4"></textarea>
            </label>
            <label class="wide">
              面试关注点
              <textarea v-model.trim="jobForm.interviewRequirements" rows="3"></textarea>
            </label>
          </div>

          <div class="action-row">
            <button class="primary-button" type="button" :disabled="loading" @click="createJob">
              <Plus :size="16" />
              创建岗位
            </button>
            <select v-model="selectedJobId" @change="selectJob">
              <option value="">选择已有岗位</option>
              <option v-for="job in jobs" :key="job.id" :value="job.id">
                {{ job.title }} / {{ job.status }}
              </option>
            </select>
          </div>
        </section>

        <section v-show="activeStep === 'candidate'" class="section-layout">
          <div class="section-title">
            <h2>候选人</h2>
            <button class="secondary-button" type="button" @click="loadCandidates">
              <ListRestart :size="16" />
              刷新候选人
            </button>
          </div>

          <div class="form-grid">
            <label>
              姓名
              <input v-model.trim="candidateForm.name" placeholder="张三" />
            </label>
            <label>
              邮箱
              <input v-model.trim="candidateForm.email" placeholder="candidate@example.com" />
            </label>
            <label>
              电话
              <input v-model.trim="candidateForm.phone" placeholder="13800000000" />
            </label>
            <label>
              来源
              <input v-model.trim="candidateForm.source" placeholder="BOSS / 内推 / 拉勾" />
            </label>
            <label class="wide">
              当前城市
              <input v-model.trim="candidateForm.currentLocation" placeholder="深圳" />
            </label>
          </div>

          <div class="action-row">
            <button class="primary-button" type="button" :disabled="loading" @click="createCandidate">
              <UserPlus :size="16" />
              创建候选人
            </button>
            <select v-model="selectedCandidateId" @change="selectCandidate">
              <option value="">选择已有候选人</option>
              <option v-for="candidate in candidates" :key="candidate.id" :value="candidate.id">
                {{ candidate.name }} / {{ candidate.email || '无邮箱' }}
              </option>
            </select>
          </div>
        </section>

        <section v-show="activeStep === 'application'" class="section-layout">
          <div class="section-title">
            <h2>申请记录</h2>
            <button class="secondary-button" type="button" @click="loadApplications">
              <ListRestart :size="16" />
              刷新申请
            </button>
          </div>

          <div class="summary-grid">
            <div class="summary-item">
              <span>当前岗位</span>
              <strong>{{ currentJob?.title || '未选择' }}</strong>
            </div>
            <div class="summary-item">
              <span>当前候选人</span>
              <strong>{{ currentCandidate?.name || '未选择' }}</strong>
            </div>
            <div class="summary-item">
              <span>当前申请</span>
              <strong>{{ currentApplication?.status || '未创建' }}</strong>
            </div>
          </div>

          <div class="action-row">
            <button class="primary-button" type="button" :disabled="loading || !currentJob || !currentCandidate" @click="createApplication">
              <FilePlus2 :size="16" />
              创建申请
            </button>
            <select v-model="selectedApplicationId" @change="selectApplication">
              <option value="">选择已有申请</option>
              <option v-for="application in applications" :key="application.id" :value="application.id">
                {{ application.candidate?.name || '候选人' }} -> {{ application.job?.title || '岗位' }} / {{ application.status }}
              </option>
            </select>
          </div>
        </section>

        <section v-show="activeStep === 'resume'" class="section-layout">
          <div class="section-title">
            <h2>简历上传</h2>
          </div>

          <div class="upload-box">
            <UploadCloud :size="28" />
            <div>
              <strong>{{ selectedFile?.name || '选择 PDF / DOCX / TXT 简历文件' }}</strong>
              <span>上传后后端会解析简历，并把文本写入 RAG 检索链路。</span>
            </div>
            <input type="file" accept=".pdf,.doc,.docx,.txt" @change="onFileChange" />
          </div>

          <div class="action-row">
            <button class="primary-button" type="button" :disabled="loading || !currentApplication || !selectedFile" @click="uploadResume">
              <Upload :size="16" />
              上传简历
            </button>
            <span class="inline-info">Resume ID: {{ resumeUpload?.resumeId || currentApplication?.resumeId || '-' }}</span>
          </div>
        </section>

        <section v-show="activeStep === 'analysis'" class="section-layout">
          <div class="section-title">
            <h2>Agent 分析</h2>
            <button class="secondary-button" type="button" :disabled="loading || !currentApplication" @click="loadAnalysis">
              <FileSearch :size="16" />
              读取最新报告
            </button>
          </div>

          <div class="action-row">
            <button class="primary-button" type="button" :disabled="loading || !currentApplication" @click="triggerAnalysis">
              <Bot :size="16" />
              触发简历分析
            </button>
            <label class="checkbox-row">
              <input v-model="forceReanalyze" type="checkbox" />
              强制重新分析
            </label>
          </div>

          <div class="analysis-grid">
            <div class="metric-box">
              <span>综合评分</span>
              <strong>{{ analysisReport?.overallScore ?? '-' }}</strong>
            </div>
            <div class="metric-box">
              <span>推荐结论</span>
              <strong>{{ analysisReport?.recommendation || '-' }}</strong>
            </div>
            <div class="metric-box">
              <span>人工复核</span>
              <strong>{{ analysisReport?.requiresHumanReview === undefined ? '-' : analysisReport.requiresHumanReview ? '需要' : '不需要' }}</strong>
            </div>
          </div>

          <pre class="json-view">{{ pretty(analysisReport || analysisTrigger || {}) }}</pre>
        </section>

        <section v-show="activeStep === 'outputs'" class="section-layout">
          <div class="section-title">
            <h2>沟通与问答</h2>
          </div>

          <div class="action-row">
            <button class="secondary-button" type="button" :disabled="loading || !currentApplication" @click="draftEmail">
              <MailPlus :size="16" />
              生成邮件草稿
            </button>
            <button class="secondary-button" type="button" :disabled="loading || !currentApplication" @click="proposeInterview">
              <CalendarPlus :size="16" />
              生成面试建议
            </button>
          </div>

          <label class="wide qa-row">
            招聘问答
            <div>
              <input v-model.trim="qaQuestion" placeholder="这个候选人有哪些风险点？" />
              <button class="primary-button" type="button" :disabled="loading || !currentApplication || !qaQuestion" @click="askQuestion">
                <MessagesSquare :size="16" />
                提问
              </button>
            </div>
          </label>

          <div class="result-grid">
            <article>
              <h3>邮件草稿</h3>
              <p class="result-title">{{ emailDraft?.subject || '暂无' }}</p>
              <pre>{{ emailDraft?.body || pretty(emailDraft || {}) }}</pre>
            </article>
            <article>
              <h3>面试建议</h3>
              <p class="result-title">{{ interviewPlan?.meetingTitle || '暂无' }}</p>
              <pre>{{ interviewPlan?.meetingNotes || pretty(interviewPlan || {}) }}</pre>
            </article>
            <article>
              <h3>问答结果</h3>
              <p class="result-title">{{ qaAnswer?.answer ? '已返回' : '暂无' }}</p>
              <pre>{{ qaAnswer?.answer || pretty(qaAnswer || {}) }}</pre>
            </article>
          </div>
        </section>
      </section>

      <aside class="state-panel">
        <div class="panel-heading">
          <Database :size="18" />
          <span>当前状态</span>
        </div>
        <dl>
          <dt>Job ID</dt>
          <dd>{{ currentJob?.id || '-' }}</dd>
          <dt>Candidate ID</dt>
          <dd>{{ currentCandidate?.id || '-' }}</dd>
          <dt>Application ID</dt>
          <dd>{{ currentApplication?.id || '-' }}</dd>
          <dt>Resume ID</dt>
          <dd>{{ resumeUpload?.resumeId || currentApplication?.resumeId || '-' }}</dd>
        </dl>
        <button class="secondary-button full-width" type="button" @click="resetWorkspace">
          <RotateCcw :size="16" />
          清空页面状态
        </button>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { computed, markRaw, onMounted, reactive, ref, watch } from 'vue';
import {
  Bot,
  BriefcaseBusiness,
  CalendarPlus,
  Database,
  FilePlus2,
  FileSearch,
  ListRestart,
  MailPlus,
  MessagesSquare,
  Plus,
  RefreshCw,
  RotateCcw,
  Upload,
  UploadCloud,
  UserPlus
} from '@lucide/vue';

const API_BASE = import.meta.env.VITE_API_BASE || '/api';
const STORAGE_KEY = 'ai-recruitment-admin-state';

const steps = [
  { key: 'job', label: '岗位', icon: markRaw(BriefcaseBusiness) },
  { key: 'candidate', label: '候选人', icon: markRaw(UserPlus) },
  { key: 'application', label: '申请', icon: markRaw(FilePlus2) },
  { key: 'resume', label: '简历', icon: markRaw(UploadCloud) },
  { key: 'analysis', label: '分析', icon: markRaw(Bot) },
  { key: 'outputs', label: '输出', icon: markRaw(MessagesSquare) }
];

const defaultState = {
  activeStep: 'job',
  selectedJobId: '',
  selectedCandidateId: '',
  selectedApplicationId: '',
  jobForm: {
    title: 'AI 应用工程师',
    jobType: 'AI_APP_ENGINEER',
    department: 'AI 平台部',
    level: 'Junior/Mid',
    requiredSkills: 'Python, FastAPI, RAG, Milvus, Redis',
    preferredSkills: 'Docker, Vue, Spring Boot',
    experienceRequirement: '熟悉大模型 API 调用、RAG 检索增强和后端接口开发，有 AI 应用落地经验优先。',
    description: '负责构建招聘场景下的 AI Agent 能力，包括简历分析、人岗匹配、邮件生成和招聘问答。',
    interviewRequirements: '重点考察 Python/FastAPI、RAG 检索链路、Prompt 设计、接口联调和工程化部署能力。'
  },
  candidateForm: {
    name: '候选人 A',
    email: 'candidate@example.com',
    phone: '13800000000',
    source: 'BOSS',
    currentLocation: '深圳'
  }
};

const restored = loadStoredState();
const activeStep = ref(restored.activeStep || defaultState.activeStep);
const selectedJobId = ref(restored.selectedJobId || '');
const selectedCandidateId = ref(restored.selectedCandidateId || '');
const selectedApplicationId = ref(restored.selectedApplicationId || '');
const jobForm = reactive({ ...defaultState.jobForm, ...(restored.jobForm || {}) });
const candidateForm = reactive({ ...defaultState.candidateForm, ...(restored.candidateForm || {}) });

const loading = ref(false);
const message = reactive({ type: 'info', text: '' });
const health = ref(null);
const jobs = ref([]);
const candidates = ref([]);
const applications = ref([]);
const currentJob = ref(null);
const currentCandidate = ref(null);
const currentApplication = ref(null);
const selectedFile = ref(null);
const resumeUpload = ref(null);
const forceReanalyze = ref(true);
const analysisTrigger = ref(null);
const analysisReport = ref(null);
const emailDraft = ref(null);
const interviewPlan = ref(null);
const qaQuestion = ref('这个候选人与岗位的匹配风险是什么？');
const qaAnswer = ref(null);

const healthClass = computed(() => {
  if (!health.value) return 'unknown';
  return health.value.status === 'UP' ? 'ok' : 'bad';
});

const healthLabel = computed(() => {
  if (!health.value) return '服务状态未知';
  return health.value.status === 'UP' ? 'Backend UP' : 'Backend 异常';
});

watch(
  [activeStep, selectedJobId, selectedCandidateId, selectedApplicationId, () => ({ ...jobForm }), () => ({ ...candidateForm })],
  saveStoredState,
  { deep: true }
);

onMounted(async () => {
  await Promise.allSettled([loadHealth(), loadJobs(), loadCandidates(), loadApplications()]);
});

function loadStoredState() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}');
  } catch {
    return {};
  }
}

function saveStoredState() {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      activeStep: activeStep.value,
      selectedJobId: selectedJobId.value,
      selectedCandidateId: selectedCandidateId.value,
      selectedApplicationId: selectedApplicationId.value,
      jobForm: { ...jobForm },
      candidateForm: { ...candidateForm }
    })
  );
}

async function apiFetch(path, options = {}) {
  const headers = options.body instanceof FormData ? options.headers : { 'Content-Type': 'application/json', ...(options.headers || {}) };
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const text = await response.text();
  const data = text ? parseJson(text) : null;
  if (!response.ok) {
    const detail = data?.message || data?.error || text || `HTTP ${response.status}`;
    throw new Error(detail);
  }
  return data;
}

function parseJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function asArray(text) {
  return text
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function pretty(value) {
  return JSON.stringify(value, null, 2);
}

function showMessage(type, text) {
  message.type = type;
  message.text = text;
}

async function runAction(label, action) {
  loading.value = true;
  showMessage('info', `${label}中...`);
  try {
    const result = await action();
    showMessage('success', `${label}成功`);
    return result;
  } catch (error) {
    showMessage('error', `${label}失败：${error.message}`);
    throw error;
  } finally {
    loading.value = false;
  }
}

async function loadHealth() {
  try {
    health.value = await apiFetch('/health');
  } catch (error) {
    health.value = { status: 'DOWN', error: error.message };
  }
}

async function loadJobs() {
  const data = await apiFetch('/jobs?page=1&pageSize=50');
  jobs.value = data.items || [];
  currentJob.value = jobs.value.find((job) => job.id === selectedJobId.value) || currentJob.value;
}

async function loadCandidates() {
  const data = await apiFetch('/candidates?page=1&pageSize=50');
  candidates.value = data.items || [];
  currentCandidate.value = candidates.value.find((candidate) => candidate.id === selectedCandidateId.value) || currentCandidate.value;
}

async function loadApplications() {
  const data = await apiFetch('/applications?page=1&pageSize=50');
  applications.value = data.items || [];
  currentApplication.value = applications.value.find((application) => application.id === selectedApplicationId.value) || currentApplication.value;
}

async function createJob() {
  const payload = {
    title: jobForm.title,
    jobType: jobForm.jobType,
    department: jobForm.department,
    level: jobForm.level,
    requiredSkillsJson: JSON.stringify(asArray(jobForm.requiredSkills)),
    preferredSkillsJson: JSON.stringify(asArray(jobForm.preferredSkills)),
    experienceRequirement: jobForm.experienceRequirement,
    description: jobForm.description,
    interviewRequirements: jobForm.interviewRequirements,
    status: 'ACTIVE'
  };
  const created = await runAction('创建岗位', () =>
    apiFetch('/jobs', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  );
  currentJob.value = created;
  selectedJobId.value = created.id;
  await loadJobs();
  activeStep.value = 'candidate';
}

async function createCandidate() {
  const created = await runAction('创建候选人', () =>
    apiFetch('/candidates', {
      method: 'POST',
      body: JSON.stringify({ ...candidateForm })
    })
  );
  currentCandidate.value = created;
  selectedCandidateId.value = created.id;
  await loadCandidates();
  activeStep.value = 'application';
}

async function createApplication() {
  const created = await runAction('创建申请', () =>
    apiFetch('/applications', {
      method: 'POST',
      body: JSON.stringify({
        jobId: currentJob.value.id,
        candidateId: currentCandidate.value.id,
        status: 'SUBMITTED',
        currentStage: 'RESUME_SCREENING'
      })
    })
  );
  currentApplication.value = created;
  selectedApplicationId.value = created.id;
  await loadApplications();
  activeStep.value = 'resume';
}

function selectJob() {
  currentJob.value = jobs.value.find((job) => job.id === selectedJobId.value) || null;
}

function selectCandidate() {
  currentCandidate.value = candidates.value.find((candidate) => candidate.id === selectedCandidateId.value) || null;
}

function selectApplication() {
  currentApplication.value = applications.value.find((application) => application.id === selectedApplicationId.value) || null;
  if (currentApplication.value?.job) {
    currentJob.value = currentApplication.value.job;
    selectedJobId.value = currentApplication.value.job.id;
  }
  if (currentApplication.value?.candidate) {
    currentCandidate.value = currentApplication.value.candidate;
    selectedCandidateId.value = currentApplication.value.candidate.id;
  }
}

function onFileChange(event) {
  selectedFile.value = event.target.files?.[0] || null;
}

async function uploadResume() {
  const formData = new FormData();
  formData.append('file', selectedFile.value);
  resumeUpload.value = await runAction('上传简历', () =>
    apiFetch(`/applications/${currentApplication.value.id}/resume`, {
      method: 'POST',
      body: formData
    })
  );
  currentApplication.value = await apiFetch(`/applications/${currentApplication.value.id}`);
  activeStep.value = 'analysis';
}

async function triggerAnalysis() {
  analysisTrigger.value = await runAction('触发分析', () =>
    apiFetch(`/applications/${currentApplication.value.id}/analysis`, {
      method: 'POST',
      body: JSON.stringify({ forceReanalyze: forceReanalyze.value })
    })
  );
  analysisReport.value = await apiFetch(`/applications/${currentApplication.value.id}/analysis`);
}

async function loadAnalysis() {
  analysisReport.value = await runAction('读取报告', () => apiFetch(`/applications/${currentApplication.value.id}/analysis`));
}

async function draftEmail() {
  emailDraft.value = await runAction('生成邮件草稿', () =>
    apiFetch(`/applications/${currentApplication.value.id}/emails/draft`, {
      method: 'POST',
      body: JSON.stringify({ emailType: 'INTERVIEW_INVITATION' })
    })
  );
}

async function proposeInterview() {
  const start = new Date(Date.now() + 24 * 60 * 60 * 1000);
  start.setMinutes(0, 0, 0);
  const end = new Date(start.getTime() + 60 * 60 * 1000);
  interviewPlan.value = await runAction('生成面试建议', () =>
    apiFetch(`/applications/${currentApplication.value.id}/interviews/propose`, {
      method: 'POST',
      body: JSON.stringify({
        interviewType: 'TECHNICAL',
        preferredTimezone: 'Asia/Shanghai',
        candidateAvailableSlots: [{ startTime: start.toISOString(), endTime: end.toISOString() }]
      })
    })
  );
}

async function askQuestion() {
  qaAnswer.value = await runAction('招聘问答', () =>
    apiFetch(`/applications/${currentApplication.value.id}/qa`, {
      method: 'POST',
      body: JSON.stringify({ question: qaQuestion.value })
    })
  );
}

function stepDone(key) {
  return {
    job: Boolean(currentJob.value),
    candidate: Boolean(currentCandidate.value),
    application: Boolean(currentApplication.value),
    resume: Boolean(resumeUpload.value || currentApplication.value?.resumeId),
    analysis: Boolean(analysisReport.value),
    outputs: Boolean(emailDraft.value || interviewPlan.value || qaAnswer.value)
  }[key];
}

function resetWorkspace() {
  localStorage.removeItem(STORAGE_KEY);
  selectedJobId.value = '';
  selectedCandidateId.value = '';
  selectedApplicationId.value = '';
  currentJob.value = null;
  currentCandidate.value = null;
  currentApplication.value = null;
  selectedFile.value = null;
  resumeUpload.value = null;
  analysisTrigger.value = null;
  analysisReport.value = null;
  emailDraft.value = null;
  interviewPlan.value = null;
  qaAnswer.value = null;
  activeStep.value = 'job';
  showMessage('info', '页面状态已清空，后端数据不会删除。');
}
</script>
