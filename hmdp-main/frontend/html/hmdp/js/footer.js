Vue.component("footBar", {
  template: `
    <div class="foot">
    <div class="foot-box" :class="{active: activeBtn === 1}" @click="toPage(1)">
      <div class="foot-view"><i class="el-icon-s-home"></i></div>
      <div class="foot-text">首页</div>
    </div>
    <div class="foot-box" :class="{active: activeBtn === 2}" @click="toPage(2)">
      <div class="foot-view"><i class="el-icon-map-location"></i></div>
      <div class="foot-text">地图</div>
    </div>
    <div class="foot-box" @click="toPage(0)">
      <img class="add-btn" src="/imgs/add.png" alt="">
    </div>
    <div class="foot-box" :class="{active: activeBtn === 3}" @click="toPage(3)" style="position: relative">
      <div class="foot-view"><i class="el-icon-chat-dot-round"></i></div>
      <div class="foot-text">消息</div>
      <span v-if="hasUnread" style="position: absolute; top: -2px; right: 6px; width: 8px; height: 8px; border-radius: 50%; background: #F63"></span>
    </div>
    <div class="foot-box" :class="{active: activeBtn === 4}" @click="toPage(4)">
      <div class="foot-view"><i class="el-icon-user"></i></div>
      <div class="foot-text">我的</div>
    </div>
  </div>
  `,
  data() {
    return {
      hasUnread: false, // 消息红点：存在未读评论/关注时展示
    }
  },
  props: ['activeBtn'],
  mounted() {
    // 轻量查询消息数量，未登录（401）时静默忽略
    axios
      .get("/message/comments")
      .then(({ data }) => {
        if (data && data.length > 0) {
          this.hasUnread = true;
        }
      })
      .catch(() => {});
  },
  methods: {
    toPage(i) {
      if (i === 0) {
        location.href = "/blog-edit.html"
      } else if (i === 1){
        location.href = "/"
      } else if (i === 2) {
        location.href = "/map.html"
      } else if (i === 3) {
        location.href = "/message.html"
      } else if (i === 4) {
        location.href = "/info.html"
      }
    }
  }
})