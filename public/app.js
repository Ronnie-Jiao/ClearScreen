(() => {
  const apps = [
    {id:'xhs',name:'小红书',icon:'小红书',cls:'xhs',skip:true,net:true,white:false},
    {id:'tb',name:'淘宝',icon:'淘',cls:'tb',skip:true,net:false,white:false},
    {id:'dy',name:'抖音',icon:'♪',cls:'dy',skip:true,net:true,white:false},
    {id:'wb',name:'微博',icon:'◎',cls:'wb',skip:false,net:true,white:false},
    {id:'bili',name:'哔哩哔哩',icon:'bili',cls:'bili',skip:false,net:false,white:true},
    {id:'zh',name:'知乎',icon:'知',cls:'zh',skip:true,net:false,white:false},
    {id:'wx',name:'微信',icon:'微',cls:'wx',skip:false,net:true,white:false},
    {id:'qq',name:'QQ',icon:'Q',cls:'qq',skip:true,net:true,white:false},
    {id:'alipay',name:'支付宝',icon:'支',cls:'alipay',skip:true,net:false,white:false},
    {id:'mt',name:'美团',icon:'美团',cls:'mt',skip:true,net:true,white:true},
    {id:'dding',name:'钉钉',icon:'钉',cls:'dding',skip:true,net:false,white:false},
    {id:'dd',name:'滴滴出行',icon:'D',cls:'dd',skip:false,net:true,white:false},
    {id:'jd',name:'京东',icon:'京东',cls:'jd',skip:true,net:true,white:false}
  ];

  const logs = [
    {app:'小红书', cls:'xhs', icon:'小红书', time:'08:32', type:'skip', action:'已跳过开屏广告'},
    {app:'淘宝', cls:'tb', icon:'淘', time:'09:05', type:'net', action:'已拦截广告请求'},
    {app:'抖音', cls:'dy', icon:'♪', time:'09:18', type:'fail', action:'处理失败'},
    {app:'支付宝', cls:'alipay', icon:'支', time:'10:21', type:'skip', action:'已跳过开屏广告'},
    {app:'微信', cls:'wx', icon:'微', time:'11:03', type:'net', action:'已拦截广告请求'},
    {app:'美团', cls:'mt', icon:'美团', time:'12:17', type:'skip', action:'已跳过开屏广告'},
    {app:'滴滴出行', cls:'dd', icon:'D', time:'13:26', type:'net', action:'已拦截广告请求'},
    {app:'哔哩哔哩', cls:'bili', icon:'bili', time:'15:04', type:'skip', action:'已跳过开屏广告'},
    {app:'京东', cls:'jd', icon:'京东', time:'16:20', type:'net', action:'已拦截广告请求'}
  ];

  const state = {
    page:'splash',
    history:[],
    master:true,
    theme:localStorage.getItem('cs-theme') || 'system',
    startup:true,
    autoUpdate:true,
    debug:false,
    permissions:{skip:true,network:true,bg:false},
    appFilter:'all',
    logFilter:'all',
    search:'',
    selectedApp:'xhs',
    toast:'',
    modal:null,
    splashTimer:null
  };

  function icon(name,size=24){
    const a = {
      back:'<path d="M15 18l-6-6 6-6" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>',
      home:'<path d="M3 11.5L12 4l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z" fill="currentColor"/>',
      apps:'<rect x="4" y="4" width="6" height="6" rx="1.5" fill="currentColor"/><rect x="14" y="4" width="6" height="6" rx="1.5" fill="currentColor"/><rect x="4" y="14" width="6" height="6" rx="1.5" fill="currentColor"/><rect x="14" y="14" width="6" height="6" rx="1.5" fill="currentColor"/>',
      settings:'<circle cx="12" cy="12" r="3" fill="none" stroke="currentColor" stroke-width="2"/><path d="M19 13.5l1.4 1.1-2 3.4-1.7-.7a7 7 0 0 1-2.2 1.3L14.3 20h-4.6l-.2-1.4a7 7 0 0 1-2.2-1.3l-1.7.7-2-3.4L5 13.5a7 7 0 0 1 0-3L3.6 9.4l2-3.4 1.7.7a7 7 0 0 1 2.2-1.3L9.7 4h4.6l.2 1.4a7 7 0 0 1 2.2 1.3l1.7-.7 2 3.4L19 10.5a7 7 0 0 1 0 3z" fill="none" stroke="currentColor" stroke-width="1.5"/>',
      chevron:'<path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>',
      clock:'<circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2"/><path d="M12 7v5l3 2" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>',
      access:'<circle cx="12" cy="5" r="2.2" fill="currentColor"/><path d="M5 9h14M12 9v10M8 21l4-7 4 7" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round"/>',
      globe:'<circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2"/><path d="M3 12h18M12 3c2.3 2.5 3.5 5.5 3.5 9S14.3 18.5 12 21M12 3C9.7 5.5 8.5 8.5 8.5 12S9.7 18.5 12 21" fill="none" stroke="currentColor" stroke-width="1.6"/>',
      battery:'<rect x="6" y="5" width="12" height="15" rx="2.5" fill="none" stroke="currentColor" stroke-width="2"/><path d="M10 2h4M10 12h4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>',
      shield:'<path d="M12 3l7 3v5c0 4.8-3 8-7 10-4-2-7-5.2-7-10V6z" fill="currentColor"/><path d="M8.5 12l2.1 2.1 4.7-4.7" fill="none" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>',
      play:'<path d="M4 5l7 7-7 7zM13 5l7 7-7 7z" fill="currentColor"/>',
      palette:'<path d="M12 3a9 9 0 1 0 0 18h2a2 2 0 0 0 0-4h-1a1.5 1.5 0 0 1 0-3h2.5A5.5 5.5 0 0 0 21 8.5C21 5.5 17 3 12 3z" fill="currentColor"/><circle cx="7.5" cy="9" r="1.2" fill="white"/><circle cx="10" cy="6.5" r="1.2" fill="white"/><circle cx="14" cy="6.5" r="1.2" fill="white"/>',
      refresh:'<path d="M20 7v5h-5M4 17v-5h5" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round"/><path d="M7 7a7 7 0 0 1 11 1l2 4M17 17a7 7 0 0 1-11-1l-2-4" fill="none" stroke="currentColor" stroke-width="2"/>',
      database:'<ellipse cx="12" cy="5" rx="7" ry="3" fill="currentColor"/><path d="M5 5v5c0 1.7 3.1 3 7 3s7-1.3 7-3V5M5 10v5c0 1.7 3.1 3 7 3s7-1.3 7-3v-5" fill="none" stroke="currentColor" stroke-width="2"/>',
      bug:'<path d="M8 9a4 4 0 0 1 8 0v6a4 4 0 0 1-8 0zM5 10h3M16 10h3M5 15h3M16 15h3M9 5L7 3M15 5l2-2M12 9v8" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>',
      info:'<circle cx="12" cy="12" r="9" fill="currentColor"/><path d="M12 10v6M12 7.2h.01" stroke="white" stroke-width="2.2" stroke-linecap="round"/>',
      search:'<circle cx="10.5" cy="10.5" r="6.5" fill="none" stroke="currentColor" stroke-width="2"/><path d="M15.5 15.5L21 21" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>',
      close:'<path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="2.3" stroke-linecap="round"/>',
      check:'<path d="M5 12l4 4 10-10" fill="none" stroke="currentColor" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"/>'
    };
    return '<svg viewBox="0 0 24 24" width="'+size+'" height="'+size+'" aria-hidden="true">'+(a[name]||'')+'</svg>';
  }

  function setTheme(){
    const root = document.documentElement;
    root.classList.remove('dark');
    const dark = state.theme === 'dark' || (state.theme==='system' && matchMedia('(prefers-color-scheme: dark)').matches);
    if(dark) root.classList.add('dark');
    localStorage.setItem('cs-theme',state.theme);
  }

  function go(page, push=true){
    if(push && state.page!==page) state.history.push(state.page);
    state.page = page;
    render();
  }
  function back(){
    state.page = state.history.pop() || 'home';
    render();
  }
  function toast(msg){
    state.toast = msg;
    render();
    setTimeout(()=>{state.toast=''; render();},1500);
  }
  function modal(type){ state.modal=type; render(); }
  function closeModal(){ state.modal=null; render(); }
  function toggle(prop){ state[prop]=!state[prop]; render(); }
  function togglePermission(key){ state.permissions[key]=!state.permissions[key]; render(); toast(state.permissions[key]?'已开启':'已关闭'); }
  function toggleApp(id,key){
    const a=apps.find(x=>x.id===id); if(!a)return;
    if(key==='white'){ a.white=!a.white; if(a.white){a.skip=false;a.net=false;} }
    else { a[key]=!a[key]; if(a[key]) a.white=false; }
    render();
  }
  // Obsolete prototype state removed.

  function topbar(title){
    return '<div class="topbar"><button class="back" data-action="back">'+icon('back',30)+'</button><h1>'+title+'</h1></div>';
  }
  function bottomNav(active){
    const item=(p,label,ic)=>'<button class="nav-item '+(active===p?'active':'')+'" data-go="'+p+'">'+icon(ic,26)+'<span>'+label+'</span></button>';
    return '<nav class="bottom-nav">'+item('home','首页','home')+item('apps','应用','apps')+item('settings','设置','settings')+'</nav>';
  }
  function sw(on,action){
    return '<button class="switch '+(on?'on':'')+'" data-action="'+action+'" aria-pressed="'+on+'"></button>';
  }
  function square(ic,tone='blue'){return '<div class="square-icon '+tone+'">'+icon(ic,27)+'</div>';}
  function appIcon(a){return '<div class="app-icon '+a.cls+'">'+a.icon+'</div>';}
  function chevron(){return icon('chevron',24);}

  function splash(){
    clearTimeout(state.splashTimer);
    state.splashTimer=setTimeout(()=>{ if(state.page==='splash') go('welcome',false); },1100);
    return '<div class="app-shell"><main class="splash">'+
      '<div class="brand-lockup"><img src="/logo.svg"><h1>净屏</h1><p>Clear<span>Screen</span></p><div class="tag">净化屏幕</div><div class="loader"></div></div>'+
      '</main></div>';
  }

  function welcome(){
    return '<div class="app-shell"><main class="screen no-nav onboard">'+
      '<div class="brand-lockup"><img src="/logo.svg"><h1>净屏</h1><p>Clear<span>Screen</span></p></div>'+
      '<div class="onboard-title">净化屏幕 · 专注生活</div>'+
      '<div class="illustration"><div class="shield">'+icon('check',82)+'</div></div>'+
      '<div class="feature-grid">'+
        '<div class="card feature">'+square('play','blue')+'<b>自动跳过</b></div>'+
        '<div class="card feature">'+square('globe','green')+'<b>网络过滤</b></div>'+
        '<div class="card feature">'+square('shield','purple')+'<b>白名单</b></div>'+
      '</div>'+
      '<button class="primary-btn" data-go="permissions">开始使用</button>'+
      '<button class="ghost-btn" data-go="home">稍后</button>'+
      '</main></div>';
  }

  function permissions(){
    const pc=(key,title,ic,tone,label)=>'<div class="card permission-card">'+square(ic,tone)+
      '<div class="permission-copy"><h3>'+title+'</h3><span class="badge">'+(state.permissions[key]?'✓ 已开启':'● 未开启')+'</span></div>'+
      '<button class="action-btn" data-perm="'+key+'">'+(state.permissions[key]?'已开启':'去开启')+'</button></div>';
    return '<div class="app-shell"><main class="screen no-nav">'+topbar('开启权限')+
      pc('skip','自动跳过权限','access','blue')+
      pc('network','网络过滤权限','globe','green')+
      pc('bg','后台运行','settings','purple')+
      '<div style="height:170px"></div><button class="primary-btn" data-go="home">继续</button>'+
      '</main></div>';
  }

  function logoHeader(){
    return '<div class="logo-row"><img src="/logo.svg"><div class="logo-name"><b>净屏</b><span>Clear<em>Screen</em></span></div><div class="tagline">净化屏幕<br>专注生活</div></div>';
  }

  function home(){
    const recent = logs.slice(0,3).map(l=>'<div class="row"><span class="log-time">'+l.time+'</span>'+appIcon({cls:l.cls,icon:l.icon})+
      '<div class="row-main"><div class="row-title">'+l.app+'</div></div><div class="'+(l.type==='fail'?'bad':'good')+'" style="font-weight:800">'+l.action+'</div></div>').join('');
    return '<div class="app-shell"><main class="screen">'+logoHeader()+
      '<section class="card status-card"><div class="status-orb">'+icon('check',52)+'</div><div class="status-copy"><h2>'+ (state.master?'净屏已开启':'净屏已暂停') +'</h2><p>自动跳过 + 网络过滤</p></div>'+sw(state.master,'master')+'</section>'+
      '<div class="stat-grid"><div class="card stat" data-go="records">'+
        '<div class="stat-icon blue">'+icon('play',28)+'</div><div class="stat-copy"><b>今日自动跳过</b><strong>28</strong><small> 次</small></div></div>'+
        '<div class="card stat" data-go="records"><div class="stat-icon green">'+icon('globe',28)+'</div><div class="stat-copy"><b>今日网络过滤</b><strong>136</strong><small> 次</small></div></div></div>'+
      '<section class="card section-card"><div class="section-head"><h3>权限与服务状态</h3><span class="link" data-go="permissions">全部正常</span></div>'+
        '<div class="row" data-go="permissions">'+square('access','blue')+'<div class="row-main"><div class="row-title">自动跳过权限</div></div><div class="row-right good">'+(state.permissions.skip?'已开启':'未开启')+chevron()+'</div></div>'+
        '<div class="row" data-go="permissions">'+square('shield','green')+'<div class="row-main"><div class="row-title">网络过滤状态</div></div><div class="row-right good">'+(state.permissions.network?'运行中':'未开启')+chevron()+'</div></div>'+
        '<div class="row" data-go="permissions">'+square('battery','purple')+'<div class="row-main"><div class="row-title">后台运行</div></div><div class="row-right '+(state.permissions.bg?'good':'warn')+'">'+(state.permissions.bg?'已允许':'建议开启')+chevron()+'</div></div>'+
      '</section>'+
      '<section class="card section-card"><div class="section-head"><h3>最近记录</h3><span class="link" data-go="records">查看全部</span></div>'+recent+'</section>'+
      '<div class="notice">💡 已为你减少广告打扰</div>'+
      '</main>'+bottomNav('home')+toastHtml()+modalHtml()+'</div>';
  }

  function appsPage(){
    let filtered=apps.filter(a=>!state.search || a.name.toLowerCase().includes(state.search.toLowerCase()));
    if(state.appFilter==='enabled') filtered=filtered.filter(a=>a.skip||a.net);
    if(state.appFilter==='white') filtered=filtered.filter(a=>a.white);
    const rows=filtered.map(a=>'<div class="app-row" data-app="'+a.id+'">'+appIcon(a)+
      '<div><div class="app-name">'+a.name+'</div>'+(a.white?'<span class="badge">白名单</span>':'')+'</div>'+
      '<div class="mini-control"><span>自动跳过</span><span class="mini-switch">'+sw(a.skip,'app:'+a.id+':skip')+'</span></div></div>').join('');
    const chip=(id,label)=>'<button class="chip '+(state.appFilter===id?'active':'')+'" data-filter="'+id+'">'+label+'</button>';
    return '<div class="app-shell"><main class="screen">'+
      '<div class="logo-row"><img src="/logo.svg"><div class="logo-name"><b>应用</b><span>Clear<em>Screen</em></span></div></div>'+
      '<div class="search">'+icon('search',24)+'<input id="searchInput" value="'+escapeHtml(state.search)+'" placeholder="搜索应用"></div>'+
      '<div class="chips">'+chip('all','全部 ('+apps.length+')')+chip('enabled','已开启')+chip('white','白名单')+'</div>'+
      '<section class="card app-list">'+rows+'</section>'+
      '</main>'+bottomNav('apps')+toastHtml()+modalHtml()+'</div>';
  }

  function appDetail(){
    const a=apps.find(x=>x.id===state.selectedApp)||apps[0];
    const related=logs.filter(l=>l.app===a.name).slice(0,3);
    const rel=related.length?related.map(l=>'<div class="row"><span class="log-time">'+l.time+'</span><div class="row-main"><div class="'+(l.type==='fail'?'bad':'good')+'" style="font-weight:800">'+l.action+'</div></div></div>').join(''):'<div class="row"><div class="row-main muted">暂无记录</div></div>';
    return '<div class="app-shell"><main class="screen no-nav">'+topbar('应用详情')+
      '<section class="card detail-hero">'+appIcon({...a,icon:a.icon})+'<div><h2>'+a.name+'</h2><span class="badge">✓ 已安装</span></div></section>'+
      '<section class="card section-card">'+
        '<div class="big-row">'+square('play','blue')+'<div class="row-main"><div class="row-title">自动跳过</div></div>'+sw(a.skip,'app:'+a.id+':skip')+'</div>'+
        '<div class="big-row">'+square('globe','green')+'<div class="row-main"><div class="row-title">网络过滤</div></div>'+sw(a.net,'app:'+a.id+':net')+'</div>'+
        '<div class="big-row">'+square('shield','purple')+'<div class="row-main"><div class="row-title">加入白名单</div></div>'+sw(a.white,'app:'+a.id+':white')+'</div>'+
      '</section>'+
      '<div class="hero-strip" data-action="toast:已开启双重去广告策略">💡 双重去广告 <span style="margin-left:auto">'+chevron()+'</span></div>'+
      '<section class="card section-card"><div class="section-head"><h3>最近记录</h3><span class="link" data-go="records">查看全部</span></div>'+rel+'</section>'+
      '<section class="card section-card"><div class="row" data-action="toast:高级设置将在正式版提供">'+square('settings','purple')+'<div class="row-main"><div class="row-title">高级设置</div></div><div class="row-right">'+chevron()+'</div></div></section>'+
      '</main>'+toastHtml()+modalHtml()+'</div>';
  }

  function records(){
    let items=logs;
    if(state.logFilter!=='all') items=items.filter(l=>l.type===state.logFilter);
    const t=(id,label)=>'<button class="filter-tab '+(state.logFilter===id?'active':'')+'" data-logfilter="'+id+'">'+label+'</button>';
    const rows=items.map(l=>'<div class="log-row">'+appIcon({cls:l.cls,icon:l.icon})+'<div class="app-name">'+l.app+'</div><div class="log-time">'+l.time+'</div><div class="log-action '+(l.type==='fail'?'fail':'')+'">'+l.action+'</div></div>').join('');
    return '<div class="app-shell"><main class="screen no-nav">'+topbar('记录')+
      '<div class="filter-tabs">'+t('all','全部')+t('skip','自动跳过')+t('net','网络过滤')+t('fail','失败')+'</div>'+
      '<section class="card app-list">'+rows+'</section><div style="text-align:center;color:var(--muted);padding:20px 0;font-weight:700">记录默认保留 7 天</div>'+
      '</main>'+toastHtml()+modalHtml()+'</div>';
  }

  function settings(){
    const seg='<div class="segment">'+['system','light','dark'].map(x=>'<button data-theme="'+x+'" class="'+(state.theme===x?'active':'')+'">'+({system:'跟随系统',light:'浅色',dark:'深色'}[x])+'</button>').join('')+'</div>';
    return '<div class="app-shell"><main class="screen">'+logoHeader()+
      '<section class="card settings-group"><div class="group-title">基础设置</div>'+
        '<div class="row">'+square('settings','blue')+'<div class="row-main"><div class="row-title">开机后恢复上次状态</div></div>'+sw(state.startup,'startup')+'</div>'+
        '<div class="row">'+square('refresh','green')+'<div class="row-main"><div class="row-title">规则自动更新</div></div>'+sw(state.autoUpdate,'autoUpdate')+'</div>'+
        '<div class="row" data-go="appearance">'+square('palette','purple')+'<div class="row-main"><div class="row-title">外观</div></div>'+seg+'</div>'+
      '</section>'+
      '<section class="card settings-group"><div class="group-title">权限与服务</div>'+
        '<div class="row" data-go="permissions">'+square('access','blue')+'<div class="row-main"><div class="row-title">自动跳过权限状态</div></div><div class="row-right good">'+(state.permissions.skip?'已开启':'未开启')+chevron()+'</div></div>'+
        '<div class="row" data-go="permissions">'+square('shield','green')+'<div class="row-main"><div class="row-title">网络过滤状态</div></div><div class="row-right good">'+(state.permissions.network?'运行中':'未开启')+chevron()+'</div></div>'+
        '<div class="row" data-go="permissions">'+square('battery','purple')+'<div class="row-main"><div class="row-title">电池 / 后台运行指引</div></div><div class="row-right">'+chevron()+'</div></div>'+
      '</section>'+
      '<section class="card settings-group"><div class="group-title">隐私与数据</div>'+
        '<div class="row" data-action="toast:日志保留周期：30天">'+square('database','blue')+'<div class="row-main"><div class="row-title">日志保留周期</div></div><div class="row-right">30 天 '+chevron()+'</div></div>'+
        '<div class="row" data-action="confirmClear">'+square('database','red')+'<div class="row-main"><div class="row-title">清空记录</div></div><div class="row-right">'+chevron()+'</div></div>'+
      '</section>'+
      ''+
      '<section class="card settings-group"><div class="group-title">高级与关于</div>'+
        '<div class="row" data-go="rules">'+square('refresh','blue')+'<div class="row-main"><div class="row-title">规则管理</div></div><div class="row-right">'+chevron()+'</div></div>'+
        '<div class="row">'+square('bug','purple')+'<div class="row-main"><div class="row-title">调试模式</div></div>'+sw(state.debug,'debug')+'</div>'+
        '<div class="row" data-action="toast:当前版本 v1.0.0">'+square('info','green')+'<div class="row-main"><div class="row-title">当前版本</div></div><div class="row-right">v1.0.0 '+chevron()+'</div></div>'+
        '<div class="row" data-action="toast:原型中的隐私政策为示意入口">'+square('info','blue')+'<div class="row-main"><div class="row-title">隐私政策</div></div><div class="row-right">'+chevron()+'</div></div>'+
      '</section>'+
      '</main>'+bottomNav('settings')+toastHtml()+modalHtml()+'</div>';
  }

  // Obsolete prototype pages removed.

  function appearance(){
    const card=(id,label,preview)=>'<button class="card" data-theme="'+id+'" style="padding:12px;min-height:176px;border:2px solid '+(state.theme===id?'#188af0':'transparent')+';cursor:pointer"><div style="height:112px;border-radius:16px;background:'+preview+';box-shadow:inset 0 0 0 1px var(--border);margin-bottom:10px"></div><b style="font-size:17px">'+label+'</b></button>';
    return '<div class="app-shell"><main class="screen no-nav">'+topbar('外观')+
      '<section class="card settings-group"><div class="group-title">'+square('palette','blue')+'主题模式</div>'+
        '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:10px;padding:14px 0">'+
          card('system','跟随系统','linear-gradient(90deg,#f4fbfd 50%,#10212e 50%)')+
          card('light','浅色','linear-gradient(#f7fbfd,#eef7fb)')+
          card('dark','深色','linear-gradient(#0b1721,#163041)')+
        '</div></section>'+
      '<section class="card settings-group"><div class="group-title">'+square('settings','blue')+'界面设置</div>'+
        '<div class="row" data-action="toast:图标风格已设为圆润"><div class="row-main"><div class="row-title">图标风格</div></div><div class="segment"><button class="active">圆润</button><button>简约</button><button>方形</button></div></div>'+
        '<div class="row" data-action="toast:卡片圆角已设为中"><div class="row-main"><div class="row-title">卡片圆角</div></div><div class="segment"><button>小</button><button class="active">中</button><button>大</button></div></div>'+
        '<div class="row"><div class="row-main"><div class="row-title">动效</div></div>'+sw(true,'noop')+'</div>'+
      '</section>'+
      '</main>'+toastHtml()+modalHtml()+'</div>';
  }

  function whitelist(){
    const rows=apps.map(a=>'<div class="app-row">'+appIcon(a)+'<div><div class="app-name">'+a.name+'</div></div><div>'+(a.white?'<span class="badge">白名单</span>':sw(false,'app:'+a.id+':white'))+'</div></div>').join('');
    return '<div class="app-shell"><main class="screen no-nav">'+topbar('白名单')+
      '<div class="search">'+icon('search',24)+'<input placeholder="搜索应用"></div>'+
      '<div class="chips"><button class="chip active">全部</button><button class="chip">白名单</button><button class="chip">推荐不处理</button></div>'+
      '<section class="card app-list">'+rows+'</section><div style="height:12px"></div><button class="primary-btn" data-action="toast:白名单已保存">保存</button>'+
      '</main>'+toastHtml()+modalHtml()+'</div>';
  }

  function rules(){
    return '<div class="app-shell"><main class="screen no-nav">'+topbar('规则管理')+
      '<div class="chips"><button class="chip active">自动更新</button><button class="chip">开屏规则</button><button class="chip">网络规则</button></div>'+
      '<section class="card settings-group">'+
        '<div class="row">'+square('database','purple')+'<div class="row-main"><div class="row-title">规则版本</div></div><div class="row-right">v1.0.0 '+chevron()+'</div></div>'+
        '<div class="row">'+square('clock','green')+'<div class="row-main"><div class="row-title">最后更新</div></div><div class="row-right">今天 08:32 '+chevron()+'</div></div>'+
        '<div class="row" data-action="toast:已是最新规则">'+square('refresh','blue')+'<div class="row-main"><div class="row-title">检查更新</div></div><div class="row-right">'+chevron()+'</div></div>'+
      '</section>'+
      '<section class="card settings-group">'+
        '<div class="row">'+square('bug','purple')+'<div class="row-main"><div class="row-title">调试模式</div></div>'+sw(state.debug,'debug')+'</div>'+
        '<div class="row" data-action="toast:规则导入功能为原型示意">'+square('refresh','blue')+'<div class="row-main"><div class="row-title">导入规则</div></div><div class="row-right">'+chevron()+'</div></div>'+
      '</section>'+
      '</main>'+toastHtml()+modalHtml()+'</div>';
  }

  function toastHtml(){ return state.toast?'<div class="toast">'+state.toast+'</div>':''; }
  function modalHtml(){
    if(!state.modal)return '';
    if(state.modal==='clear') return '<div class="modal-backdrop"><div class="sheet"><h3>清空全部记录？</h3><p>此操作只影响原型中的历史记录展示，不会修改系统权限。</p><div class="sheet-actions"><button class="cancel" data-action="closeModal">取消</button><button class="danger" data-action="clearDone">确认清空</button></div></div></div>';
    // Obsolete purchase sheet removed.
    return '';
  }

  function escapeHtml(s){ return String(s).replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m])); }

  function render(){
    setTheme();
    const views={splash, welcome, permissions, home, apps:appsPage, appDetail, records, settings, appearance, whitelist, rules};
    document.getElementById('app').innerHTML=(views[state.page]||home)();
    bind();
  }

  function bind(){
    document.querySelectorAll('[data-go]').forEach(el=>el.addEventListener('click',e=>{e.stopPropagation();go(el.dataset.go);}));
    document.querySelectorAll('[data-action]').forEach(el=>el.addEventListener('click',e=>{
      e.stopPropagation(); const a=el.dataset.action;
      if(a==='back') return back();
      if(a==='master') return toggle('master');
      if(a==='startup') return toggle('startup');
      if(a==='autoUpdate') return toggle('autoUpdate');
      if(a==='debug') return toggle('debug');
      if(a==='confirmClear') return modal('clear');
      if(a==='closeModal') return closeModal();
      if(a==='clearDone'){ closeModal(); return toast('记录已清空'); }
      if(a==='noop') return toast('已更新');
      if(a.startsWith('toast:')) return toast(a.slice(6));
      if(a.startsWith('app:')){
        const [,id,key]=a.split(':'); return toggleApp(id,key);
      }
    }));
    document.querySelectorAll('[data-perm]').forEach(el=>el.addEventListener('click',()=>togglePermission(el.dataset.perm)));
    document.querySelectorAll('[data-filter]').forEach(el=>el.addEventListener('click',()=>{state.appFilter=el.dataset.filter;render();}));
    document.querySelectorAll('[data-logfilter]').forEach(el=>el.addEventListener('click',()=>{state.logFilter=el.dataset.logfilter;render();}));
    document.querySelectorAll('[data-theme]').forEach(el=>el.addEventListener('click',e=>{e.stopPropagation();state.theme=el.dataset.theme;render();}));
    document.querySelectorAll('[data-app]').forEach(el=>el.addEventListener('click',()=>{state.selectedApp=el.dataset.app;go('appDetail');}));
    const input=document.getElementById('searchInput');
    if(input) input.addEventListener('input',e=>{state.search=e.target.value;const pos=e.target.selectionStart;render();const n=document.getElementById('searchInput'); if(n){n.focus();n.setSelectionRange(pos,pos);}});
  }

  matchMedia('(prefers-color-scheme: dark)').addEventListener?.('change',()=>{if(state.theme==='system')render();});
  render();
})();