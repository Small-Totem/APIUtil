## 色图获取器
这其实是我做了挺久的一个东西，但一直没弄到github上面来  
最近没啥动力往上面糊新东西了，于是打算传上来记录一下，就这样吧  
      
其实一开始其实只是打算开个空Activity研究一下api的调用啥的。。不知不觉就越搞越多了  

### 说明
- 使用Material Design设计  
- 单Activity+多Fragment,为了不卡顿使用`show`+`hide`而非`replace`控制  
- 使用`layout_behavior`，增大内容显示区域
- 收藏界面使用`RecyclerView`的瀑布流形式，数据使用SQLite存储
- 所有图片均来自pixiv(使用[pixiv.cat反向代理](https://pixiv.cat/))或api(如[LoliconApi](https://api.lolicon.app/#/setu))
- 调用的api在tips里已注明
- 仅支持android 9 及更高

### 功能&截图

#### 主菜单
<img src=imgs/Screenshot_2021-08-02-17-09-02-284_com.zjh.apiutil.jpg width=65% />

#### 获取色图
调用api,获取色图  
<img src=imgs/Screenshot_2021-08-02-17-09-51-924_com.zjh.apiutil.jpg width=65% />

#### 收藏
只能收藏pixiv的图片(有对应pid)
<img src=imgs/Screenshot_2021-08-02-17-11-42-744_com.zjh.apiutil.jpg width=65% />