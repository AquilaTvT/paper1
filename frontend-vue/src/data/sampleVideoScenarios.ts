export interface SampleVideoScenario {
  scenarioId: string;
  title: string;
  description: string;
  keyEvents: string[];
  possibleObjects: string[];
  summaryTemplate: string;
  qaHints: string[];
  keywords: string[];
}

export const sampleVideoScenarios: SampleVideoScenario[] = [
  {
    scenarioId: 'online-course',
    title: '在线课程讲解视频',
    description: '面向在线教学、课堂录播和知识点讲解的演示场景。',
    keyEvents: ['教师引入课程主题与学习目标', '围绕课件或板书讲解核心概念', '通过示例题或案例进行推导说明', '结尾回顾重点并布置后续任务'],
    possibleObjects: ['教师', '课件', '黑板', '公式', '示例题', '课堂屏幕'],
    summaryTemplate: '视频主要呈现一段课程讲解过程，内容围绕知识点说明、示例演示和阶段性总结展开。画面中可能包含教师、课件、板书或课堂屏幕，整体节奏适合提取学习重点与章节脉络。',
    qaHints: ['适合回答“本节课讲了哪些知识点”', '可提取概念定义、推导步骤和课后任务', '如用户关注某个术语，应优先整理相关解释'],
    keywords: ['course', 'class', 'lecture', 'lesson', 'teacher', 'online', '课程', '课堂', '讲解', '教学', '网课', '老师'],
  },
  {
    scenarioId: 'meeting-record',
    title: '会议记录视频',
    description: '面向工作会议、组会纪要和讨论过程归纳的演示场景。',
    keyEvents: ['主持人说明会议议题', '参会成员依次汇报进展', '围绕问题进行讨论与决策', '明确后续分工、时间节点和待办事项'],
    possibleObjects: ['会议桌', '投影屏幕', '参会人员', '笔记本电脑', '白板', '会议资料'],
    summaryTemplate: '视频整体接近会议记录场景，内容可能包括议题引入、成员发言、问题讨论和任务分配。摘要应突出结论、责任人、时间节点以及用户问题相关的讨论点。',
    qaHints: ['适合回答“会议结论是什么”', '可整理待办事项、负责人和风险点', '如用户询问某成员，应聚焦其发言与分工'],
    keywords: ['meeting', 'minutes', 'discussion', 'conference', 'report', '会议', '纪要', '讨论', '汇报', '组会'],
  },
  {
    scenarioId: 'security-monitoring',
    title: '安防监控片段',
    description: '面向固定机位监控、出入口巡检和异常事件提示的演示场景。',
    keyEvents: ['固定视角记录场地或通道状态', '人员或车辆进入画面并发生位置变化', '可能出现停留、经过、离开等行为', '对异常停留、越界或聚集进行提示'],
    possibleObjects: ['行人', '车辆', '门禁', '走廊', '摄像头时间戳', '围栏', '货架'],
    summaryTemplate: '视频可被归纳为一段安防监控片段，核心关注画面中的人员或车辆移动、停留和离开顺序。摘要应保持客观，重点描述可能的关键事件与异常提示，而不夸大识别结果。',
    qaHints: ['适合回答“是否有异常事件”', '可按时间顺序描述人员或车辆活动', '如用户关注安全风险，应列出需复核的片段'],
    keywords: ['security', 'surveillance', 'camera', 'monitor', 'parking', '安防', '监控', '摄像头', '异常', '出入口', '车辆'],
  },
  {
    scenarioId: 'short-video-material',
    title: '短视频素材',
    description: '面向 vlog、产品展示、生活记录和社交媒体素材整理的演示场景。',
    keyEvents: ['开头通过场景或人物引入主题', '中段展示动作、产品或生活片段', '通过转场形成节奏变化', '结尾给出情绪收束或行动提示'],
    possibleObjects: ['人物主体', '手机', '产品', '街景', '室内布景', '字幕贴纸', '背景音乐提示'],
    summaryTemplate: '视频整体更像短视频素材，内容可能包含人物展示、物品特写、环境切换和节奏化剪辑。摘要应突出主题、亮点镜头、可用于剪辑的片段以及与用户指令相关的信息。',
    qaHints: ['适合回答“有哪些可剪辑亮点”', '可提取开头、高潮和结尾段落', '如用户关注传播效果，应归纳主题和情绪风格'],
    keywords: ['vlog', 'short', 'clip', 'tiktok', 'douyin', 'reel', '短视频', '素材', '剪辑', 'vlog', '抖音'],
  },
  {
    scenarioId: 'graduation-demo',
    title: '毕业设计演示样例视频',
    description: '面向本科毕业设计系统演示、论文截图和答辩 PPT 的样例场景。',
    keyEvents: ['打开多模态视频理解与摘要系统首页', '选择样例视频并输入摘要或问答指令', '展示异步任务状态、SSE 流式输出和历史记录', '呈现 196 → 5 的双轨 Token 压缩指标', '说明 backend mode 可对接 Java、Redis 与 Python 推理服务'],
    possibleObjects: ['系统首页', '上传卡片', '任务时间线', '摘要输出区域', 'Token 压缩卡片', '历史记录表格'],
    summaryTemplate: '视频展示的是毕业设计系统的功能演示流程，重点包括视频选择、任务创建、异步推理状态流转、流式摘要输出以及 Token 压缩指标展示。该场景适合放入论文第六章界面展示和答辩 PPT。',
    qaHints: ['适合回答“系统演示了哪些功能模块”', '可说明 mock mode 与 backend mode 的边界', '可围绕论文创新点解释 Token 压缩效果'],
    keywords: ['graduation', 'demo', 'thesis', 'design', 'system', '毕业设计', '演示', '答辩', '论文', '系统'],
  },
];

export const genericVideoScenario: SampleVideoScenario = {
  scenarioId: 'generic-video',
  title: '通用视频摘要场景',
  description: '当文件名和用户指令无法明显匹配时使用的保守 mock 场景。',
  keyEvents: ['读取浏览器可获得的文件元数据', '结合用户指令选择通用摘要结构', '按时间顺序组织可能的内容变化', '补充 mock mode 下的系统处理说明'],
  possibleObjects: ['人物', '场景背景', '屏幕内容', '物体', '动作变化'],
  summaryTemplate: '当前视频未能从文件名或指令中明确判断具体类别，因此摘要采用通用结构：先概括可能的主题，再按时间顺序整理关键事件，并单独说明 mock mode 的处理边界。',
  qaHints: ['适合回答通用摘要、关键事件和系统指标问题', '应避免宣称已经真实识别画面内容', '可提示切换 backend mode 获取真实解码推理能力'],
  keywords: [],
};

export function selectScenario(fileName: string, instruction: string, preferGraduationDemo = false): SampleVideoScenario {
  if (preferGraduationDemo) return sampleVideoScenarios.find((scenario) => scenario.scenarioId === 'graduation-demo') ?? genericVideoScenario;

  const source = `${fileName} ${instruction}`.toLowerCase();
  const scored = sampleVideoScenarios
    .map((scenario) => ({
      scenario,
      score: scenario.keywords.reduce((total, keyword) => (source.includes(keyword.toLowerCase()) ? total + 1 : total), 0),
    }))
    .sort((a, b) => b.score - a.score);

  return scored[0]?.score > 0 ? scored[0].scenario : genericVideoScenario;
}
