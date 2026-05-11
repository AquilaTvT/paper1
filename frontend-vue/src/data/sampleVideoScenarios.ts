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
    scenarioId: 'light-switch',
    title: '灯开关短视频',
    description: '画面聚焦手臂、墙面开关和一次按压动作。',
    keyEvents: ['画面中出现墙面上的白色灯开关', '一只手臂靠近开关位置', '手指按下开关并完成一次操作', '按压后场景亮度可能出现变化'],
    possibleObjects: ['手臂', '手指', '白色灯开关', '墙面', '室内光线'],
    summaryTemplate: '画面主要展示一只手臂靠近墙面白色灯开关，并完成按压动作。按压前后如出现亮度变化，可理解为一次开灯或关灯操作；视频内容较短，重点是单次开关控制动作。',
    qaHints: ['可描述手臂靠近、手指按压和动作结束', '可关注按压前后的亮度变化', '对开灯或关灯只作谨慎判断'],
    keywords: ['switch', 'light', 'lamp', 'lightswitch', 'turn on', 'turn off', '开关', '灯', '电灯', '灯光', '按开关', '开灯', '关灯'],
  },
  {
    scenarioId: 'online-course',
    title: '在线课程讲解视频',
    description: '课程讲解、课件展示或板书讲述类视频。',
    keyEvents: ['教师引入课程主题与学习目标', '围绕课件或板书讲解核心概念', '通过示例题或案例进行说明', '结尾回顾重点并安排后续学习内容'],
    possibleObjects: ['教师', '课件', '黑板', '公式', '示例题', '课堂屏幕'],
    summaryTemplate: '视频主要呈现一段课程讲解过程，内容围绕知识点说明、示例推导和阶段性总结展开。摘要应优先提取主题、概念、推导过程和学习重点。',
    qaHints: ['适合回答“本节课讲了哪些知识点”', '可提取概念定义、推导步骤和课后任务', '如用户关注某个术语，应优先整理相关解释'],
    keywords: ['course', 'class', 'lecture', 'lesson', 'teacher', 'online', '课程', '课堂', '讲解', '教学', '网课', '老师'],
  },
  {
    scenarioId: 'meeting-record',
    title: '会议记录视频',
    description: '会议、汇报或小组讨论类视频。',
    keyEvents: ['主持人说明会议议题', '参会成员依次汇报进展', '围绕问题进行讨论与决策', '明确后续分工、时间节点和待办事项'],
    possibleObjects: ['会议桌', '投影屏幕', '参会人员', '笔记本电脑', '白板', '会议资料'],
    summaryTemplate: '视频整体接近会议记录场景，内容可能包括议题引入、成员发言、问题讨论和任务分配。摘要应突出结论、责任人、时间节点以及用户问题相关的讨论点。',
    qaHints: ['适合回答“会议结论是什么”', '可整理待办事项、负责人和风险点', '如用户询问某成员，应聚焦其发言与分工'],
    keywords: ['meeting', 'minutes', 'discussion', 'conference', 'report', '会议', '纪要', '讨论', '汇报', '组会'],
  },
  {
    scenarioId: 'security-monitoring',
    title: '安防监控片段',
    description: '固定机位下的人、车或通道状态记录。',
    keyEvents: ['固定视角记录场地或通道状态', '人员或车辆进入画面并发生位置变化', '可能出现停留、经过、离开等行为', '对异常停留、越界或聚集进行提示'],
    possibleObjects: ['行人', '车辆', '门禁', '走廊', '时间戳', '围栏', '货架'],
    summaryTemplate: '视频可被归纳为一段安防监控片段，核心关注画面中的人员或车辆移动、停留和离开顺序。摘要应保持客观，重点描述关键事件与需复核的异常线索。',
    qaHints: ['适合回答“是否有异常事件”', '可按时间顺序描述人员或车辆活动', '如用户关注安全风险，应列出需复核的片段'],
    keywords: ['security', 'surveillance', 'camera', 'monitor', 'parking', '安防', '监控', '摄像头', '异常', '出入口', '车辆'],
  },
  {
    scenarioId: 'short-video-material',
    title: '短视频素材',
    description: '生活记录、产品展示或社交媒体素材。',
    keyEvents: ['开头通过场景或人物引入主题', '中段展示动作、产品或生活片段', '通过转场形成节奏变化', '结尾给出情绪收束或行动提示'],
    possibleObjects: ['人物主体', '手机', '产品', '街景', '室内布景', '字幕贴纸', '背景音乐提示'],
    summaryTemplate: '视频整体更像短视频素材，内容可能包含人物展示、物品特写、环境切换和节奏化剪辑。摘要应突出主题、亮点镜头和与用户指令相关的信息。',
    qaHints: ['适合回答“有哪些可剪辑亮点”', '可提取开头、高潮和结尾段落', '如用户关注传播效果，应归纳主题和情绪风格'],
    keywords: ['vlog', 'short', 'clip', 'tiktok', 'douyin', 'reel', '短视频', '素材', '剪辑', '抖音'],
  },
];

export const genericVideoScenario: SampleVideoScenario = {
  scenarioId: 'generic-video',
  title: '通用视频摘要场景',
  description: '文件名和用户指令没有明显类别时使用的保守摘要场景。',
  keyEvents: ['读取视频文件', '提取基础元数据', '采样关键帧', '分析动作片段', '生成摘要'],
  possibleObjects: ['人物', '场景背景', '屏幕内容', '物体', '动作变化'],
  summaryTemplate: '当前视频缺少明确场景线索，摘要采用保守表达：先概括可能的主题，再按时间顺序整理关键事件，并避免对无法确认的细节作绝对判断。',
  qaHints: ['适合回答通用摘要和关键事件问题', '应避免过度推断画面细节', '可结合用户指令突出相关片段'],
  keywords: [],
};

export function selectScenario(fileName: string, instruction: string, preferSample = false): SampleVideoScenario {
  const source = `${fileName} ${instruction}`.toLowerCase();
  const lightSwitchScenario = sampleVideoScenarios.find((scenario) => scenario.scenarioId === 'light-switch');

  if (lightSwitchScenario?.keywords.some((keyword) => source.includes(keyword.toLowerCase()))) {
    return lightSwitchScenario;
  }

  if (preferSample) return lightSwitchScenario ?? genericVideoScenario;

  const scored = sampleVideoScenarios
    .map((scenario) => ({
      scenario,
      score: scenario.keywords.reduce((total, keyword) => (source.includes(keyword.toLowerCase()) ? total + 1 : total), 0),
    }))
    .sort((a, b) => b.score - a.score);

  return scored[0]?.score > 0 ? scored[0].scenario : genericVideoScenario;
}
