USE ai_recruitment;

INSERT INTO job_templates (
  id,
  title,
  job_type,
  required_skills_json,
  preferred_skills_json,
  experience_requirement,
  description,
  interview_requirements
) VALUES
(
  'tpl-ai-application-engineer',
  'AI 应用工程师',
  'AI_APPLICATION_ENGINEER',
  JSON_ARRAY('Python', 'LLM', 'RAG', 'Prompt Engineering', 'API Integration'),
  JSON_ARRAY('LangChain', 'Milvus', 'FastAPI', 'Docker', 'MLOps'),
  '具备 AI 应用开发经验，理解大模型 API、RAG 和工程化落地。',
  '负责构建基于大模型的业务应用、智能体流程和 RAG 检索能力。',
  '重点考察 LLM API 接入、RAG 原理、Python 工程能力和项目落地经验。'
),
(
  'tpl-java-backend-engineer',
  'Java 后端工程师',
  'JAVA_BACKEND_ENGINEER',
  JSON_ARRAY('Java', 'Spring Boot', 'MySQL', 'REST API', 'Redis'),
  JSON_ARRAY('Docker', 'Kafka', 'Spring Cloud', 'JUnit', 'CI/CD'),
  '具备 Java 后端开发经验，熟悉常见 Web 后端架构和数据库设计。',
  '负责后端 API、业务流程、数据库访问和系统集成。',
  '重点考察 Java 基础、Spring Boot、数据库设计、接口设计和项目经验。'
),
(
  'tpl-python-backend-engineer',
  'Python 后端工程师',
  'PYTHON_BACKEND_ENGINEER',
  JSON_ARRAY('Python', 'FastAPI', 'SQL', 'REST API', 'Async Programming'),
  JSON_ARRAY('Celery', 'Redis', 'Docker', 'Pytest', 'Pydantic'),
  '具备 Python 后端服务开发经验，熟悉 API、数据库和异步任务。',
  '负责 Python 后端服务、内部 API、任务处理和第三方系统集成。',
  '重点考察 Python 工程能力、FastAPI、数据库访问、异步任务和测试经验。'
),
(
  'tpl-frontend-engineer',
  '前端工程师',
  'FRONTEND_ENGINEER',
  JSON_ARRAY('JavaScript', 'TypeScript', 'React', 'HTML', 'CSS'),
  JSON_ARRAY('Next.js', 'Vite', 'Testing Library', 'E2E Testing', 'Design System'),
  '具备现代前端工程经验，能够独立实现管理后台和业务交互。',
  '负责前端页面、组件、状态管理、接口联调和用户体验优化。',
  '重点考察 React/TypeScript、组件设计、接口联调和工程化实践。'
),
(
  'tpl-test-engineer',
  '测试工程师',
  'TEST_ENGINEER',
  JSON_ARRAY('Test Case Design', 'API Testing', 'SQL', 'Bug Tracking', 'Automation Testing'),
  JSON_ARRAY('Playwright', 'Selenium', 'JMeter', 'Pytest', 'CI/CD'),
  '具备软件测试经验，理解接口测试、自动化测试和质量保障流程。',
  '负责测试计划、用例设计、自动化测试和缺陷跟踪。',
  '重点考察测试思维、接口测试、自动化能力和问题定位能力。'
),
(
  'tpl-algorithm-engineer',
  '算法工程师',
  'ALGORITHM_ENGINEER',
  JSON_ARRAY('Python', 'Machine Learning', 'Deep Learning', 'Data Processing', 'Model Evaluation'),
  JSON_ARRAY('PyTorch', 'TensorFlow', 'NLP', 'Recommendation System', 'Feature Engineering'),
  '具备机器学习或深度学习经验，能够完成模型训练、评估和优化。',
  '负责算法模型设计、实验、评估和业务场景落地。',
  '重点考察算法基础、模型评估、数据处理和项目落地经验。'
),
(
  'tpl-data-engineer',
  '数据工程师',
  'DATA_ENGINEER',
  JSON_ARRAY('SQL', 'Python', 'Data Modeling', 'ETL', 'Data Warehouse'),
  JSON_ARRAY('Spark', 'Flink', 'Airflow', 'Kafka', 'Lakehouse'),
  '具备数据开发经验，熟悉数据建模、ETL 和数据质量管理。',
  '负责数据链路、数据仓库、指标加工和数据服务。',
  '重点考察 SQL、数据建模、ETL、数据质量和大数据组件经验。'
),
(
  'tpl-big-data-engineer',
  '大数据工程师',
  'BIG_DATA_ENGINEER',
  JSON_ARRAY('Java', 'Scala', 'Spark', 'Hive', 'Distributed Systems'),
  JSON_ARRAY('Flink', 'Kafka', 'Hadoop', 'HBase', 'Data Lake'),
  '具备大数据平台或实时计算开发经验，理解分布式系统基础。',
  '负责大数据计算任务、数据平台和实时数据处理。',
  '重点考察 Spark/Flink、分布式计算、数据链路和性能优化经验。'
),
(
  'tpl-devops-engineer',
  '运维 / DevOps 工程师',
  'DEVOPS_ENGINEER',
  JSON_ARRAY('Linux', 'Docker', 'CI/CD', 'Shell', 'Monitoring'),
  JSON_ARRAY('Kubernetes', 'Terraform', 'Prometheus', 'Grafana', 'Cloud Platform'),
  '具备系统运维、容器化和自动化部署经验。',
  '负责部署、监控、自动化运维、环境管理和稳定性保障。',
  '重点考察 Linux、Docker、CI/CD、监控告警和故障排查能力。'
),
(
  'tpl-fullstack-engineer',
  '全栈工程师',
  'FULLSTACK_ENGINEER',
  JSON_ARRAY('Frontend Framework', 'Backend API', 'Database', 'Git', 'System Design'),
  JSON_ARRAY('React', 'Spring Boot', 'FastAPI', 'Docker', 'Cloud Deployment'),
  '具备前后端协作开发经验，能够独立完成小型业务闭环。',
  '负责前端页面、后端接口、数据库设计和端到端交付。',
  '重点考察前后端基础、接口设计、数据库设计和独立交付能力。'
),
(
  'tpl-architect',
  '架构师',
  'ARCHITECT',
  JSON_ARRAY('System Design', 'Distributed Systems', 'Database Design', 'Architecture Governance', 'Technical Leadership'),
  JSON_ARRAY('Cloud Native', 'Microservices', 'Observability', 'Security', 'Performance Optimization'),
  '具备复杂系统架构设计和技术决策经验，能够指导团队落地。',
  '负责系统架构、技术选型、工程规范和关键技术风险控制。',
  '重点考察系统设计、架构权衡、性能稳定性、安全和团队技术影响力。'
)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  job_type = VALUES(job_type),
  required_skills_json = VALUES(required_skills_json),
  preferred_skills_json = VALUES(preferred_skills_json),
  experience_requirement = VALUES(experience_requirement),
  description = VALUES(description),
  interview_requirements = VALUES(interview_requirements),
  active = 1,
  updated_at = CURRENT_TIMESTAMP(3);

