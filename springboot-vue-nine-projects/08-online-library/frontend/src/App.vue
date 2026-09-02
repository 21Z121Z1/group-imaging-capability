<script setup>
import {computed,onMounted,ref} from 'vue'; import {api,session} from './api';
const username=ref('admin'),password=ref('Admin123!Demo'),user=ref(null),rows=ref([]),error=ref(''),busy=ref(false),notice=ref('');
const count=computed(()=>rows.value.length);
async function login(){try{busy.value=true;error.value='';const r=await api('/api/auth/login',{method:'POST',body:JSON.stringify({username:username.value,password:password.value})});session.token=r.token;user.value=r.user;await load();}catch(e){error.value=e.message}finally{busy.value=false}}
async function load(){try{rows.value=await api('/api/books')}catch(e){if(e.message.includes('401'))logout();else error.value=e.message}}
async function me(){if(!session.token)return;try{user.value=await api('/api/auth/me');await load()}catch{session.token=''}}
function logout(){session.token='';user.value=null;rows.value=[]}
async function quick(){try{busy.value=true;notice.value='';await api('/api/borrows',{method:'POST',body:JSON.stringify({bookId:1})});notice.value='操作已提交';await load()}catch(e){error.value=e.message}finally{busy.value=false}}
onMounted(me);
</script>
<template>
  <div class="app-shell">
    <div class="catalog-line">CATALOGUE NO. 08 · READING ROOM</div>
    <main class="main">
      <header class="top"><div><p class="eyebrow">在线借阅与馆藏流转</p><h1>藏书室</h1></div><div v-if="user" class="account"><span>{{user.displayName}} · {{user.role}}</span><button class="ghost" @click="logout">退出</button></div></header>
      <section v-if="!user" class="login-card"><div><small>DEMO ACCESS</small><h2>进入藏书室</h2><p>管理员：admin / Admin123!Demo<br>普通用户：student / Student123!Demo</p></div><form @submit.prevent="login"><input v-model="username" placeholder="用户名"><input v-model="password" type="password" placeholder="密码"><button :disabled="busy">登录</button></form></section>
      <template v-else>
        <section class="hero"><div><span class="kicker">馆藏图书</span><strong>{{count}}</strong><p>当前数据条目</p></div><div class="hero-copy"><h2>线上图书借阅管理平台</h2><p>后端运行于 :8108，前端开发端口 :5108。当前页面直接读取真实 REST API。</p><button @click="quick" :disabled="busy">借阅 1 号图书</button></div></section>
        <p v-if="notice" class="notice">{{notice}}</p><p v-if="error" class="error">{{error}}</p>
        <section class="panel"><div class="panel-head"><h3>馆藏图书</h3><button class="ghost" @click="load">刷新</button></div><div class="cards"><article v-for="row in rows" :key="row.id" class="data-card"><span class="id">#{{row.id}}</span><h4>{{row.name || row.title || row.code || row.isbn || ('记录 '+row.id)}}</h4><p>{{row.description || row.category || row.address || row.author || row.building || row.status || '暂无描述'}}</p><small>{{row.status || row.type || row.role || 'ACTIVE'}}</small></article><div v-if="!rows.length" class="empty">暂无数据</div></div></section>
      </template>
    </main>
  </div>
</template>
