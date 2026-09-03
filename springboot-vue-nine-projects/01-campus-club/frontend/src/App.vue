<script setup>
import { computed, ref } from 'vue';
import { ApiError, api, session } from './api';

const username = ref('');
const password = ref('');
const user = ref(null);
const rows = ref([]);
const selectedClubId = ref(null);
const motivation = ref('');
const error = ref('');
const notice = ref('');
const busy = ref(false);
const count = computed(() => rows.value.length);

function logout(message = '') {
  session.token = '';
  user.value = null;
  rows.value = [];
  selectedClubId.value = null;
  if (message) error.value = message;
}
function fail(e) {
  if (e instanceof ApiError && e.status === 401) return logout('会话已失效，请重新登录');
  error.value = e?.message || '操作失败，请重试';
}
async function load() {
  try {
    rows.value = await api('/api/clubs');
    if (!rows.value.some(x => x.id === selectedClubId.value)) selectedClubId.value = rows.value[0]?.id ?? null;
  } catch (e) { fail(e); }
}
async function login() {
  if (!username.value.trim() || !password.value) return;
  busy.value = true; error.value = ''; notice.value = '';
  try {
    const r = await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ username: username.value.trim(), password: password.value }) });
    session.token = r.token; user.value = r.user; password.value = ''; await load();
  } catch (e) { fail(e); } finally { busy.value = false; }
}
async function applyMembership() {
  if (!selectedClubId.value || !motivation.value.trim()) return;
  busy.value = true; error.value = ''; notice.value = '';
  try {
    await api('/api/memberships/apply', { method: 'POST', body: JSON.stringify({ clubId: selectedClubId.value, motivation: motivation.value.trim() }) });
    motivation.value = ''; notice.value = '入社申请已提交。'; await load();
  } catch (e) { fail(e); } finally { busy.value = false; }
}
</script>

<template>
  <div class="app-shell">
    <aside class="side"><div class="mark">C</div><span>概览</span><span>社团</span><span>活动</span><span>成员</span></aside>
    <main class="main">
      <header class="top"><div><p class="eyebrow">校园组织 · 活动 · 成员</p><h1>社团中枢</h1></div><div v-if="user" class="account"><span>{{ user.displayName }} · {{ user.role }}</span><button class="ghost" @click="logout()">退出</button></div></header>
      <section v-if="!user" class="login-card"><div><small>SECURE ACCESS</small><h2>进入社团中枢</h2><p>使用已授权的校园账号登录。凭据不会写入浏览器持久存储。</p></div><form @submit.prevent="login"><label>用户名<input v-model.trim="username" autocomplete="username" maxlength="32" required placeholder="用户名"></label><label>密码<input v-model="password" type="password" autocomplete="current-password" minlength="12" maxlength="128" required placeholder="密码"></label><button :disabled="busy">{{ busy ? '登录中…' : '登录' }}</button></form></section>
      <template v-else>
        <section class="hero"><div><span class="kicker">社团</span><strong>{{ count }}</strong><p>当前可见社团</p></div><div class="hero-copy"><h2>高校社团管理系统</h2><p>浏览社团信息并提交可追踪的入社申请。所有写操作均通过当前登录身份执行。</p></div></section>
        <section class="action-panel" aria-labelledby="join-title"><div><span class="kicker">MEMBERSHIP</span><h3 id="join-title">申请加入社团</h3><p>选择目标社团并填写真实申请理由。</p></div><form @submit.prevent="applyMembership"><label>目标社团<select v-model.number="selectedClubId" required><option disabled :value="null">请选择社团</option><option v-for="club in rows" :key="club.id" :value="club.id">{{ club.name }} · #{{ club.id }}</option></select></label><label>申请理由<textarea v-model.trim="motivation" maxlength="1000" required rows="3" placeholder="说明加入动机、经验或希望参与的活动"></textarea></label><button :disabled="busy || !selectedClubId">提交申请</button></form></section>
        <p v-if="notice" class="notice" role="status">{{ notice }}</p><p v-if="error" class="error" role="alert">{{ error }}</p>
        <section class="panel"><div class="panel-head"><h3>社团</h3><button class="ghost" :disabled="busy" @click="load">刷新</button></div><div class="cards"><article v-for="row in rows" :key="row.id" class="data-card"><span class="id">#{{ row.id }}</span><h4>{{ row.name || ('社团 ' + row.id) }}</h4><p>{{ row.description || row.category || '暂无描述' }}</p><small>{{ row.status || 'ACTIVE' }}</small></article><div v-if="!rows.length" class="empty">暂无可见社团</div></div></section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.action-panel{display:grid;grid-template-columns:minmax(220px,.65fr) minmax(300px,1.35fr);gap:28px;margin:0 0 28px;padding:28px;border-radius:24px;background:#ffffffd9;border:1px solid #0000000d}.action-panel h3{margin:.35rem 0}.action-panel p{color:#64748b}.action-panel label,.login-card label{display:grid;gap:7px;font-size:.86rem;font-weight:700}.action-panel select,.action-panel textarea{width:100%;padding:13px 15px;border:1px solid #cbd5e1;border-radius:12px;background:#fff;color:inherit;font:inherit}.action-panel textarea{resize:vertical}.action-panel button:disabled,.login-card button:disabled{opacity:.55;cursor:not-allowed}@media(max-width:760px){.action-panel{grid-template-columns:1fr}}
</style>
