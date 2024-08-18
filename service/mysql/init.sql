CREATE DATABASE IF NOT EXISTS medllm
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;
# 设置中国时区
SET time_zone = '+8:00';
# 选择数据库
USE medllm;
# 设置通讯字符集
SET NAMES utf8mb4;

GRANT ALL PRIVILEGES ON medllm.* TO 'admin'@'%';
FLUSH PRIVILEGES;

# 用户表
CREATE TABLE User (
    UserId INT AUTO_INCREMENT PRIMARY KEY,                -- 自增长的用户ID
    Username VARCHAR(50) NOT NULL UNIQUE,                 -- 用户名，必须唯一且非空
    Password VARCHAR(255) NOT NULL,                       -- 密码，非空
    Email VARCHAR(100) NOT NULL UNIQUE,                   -- 电子邮件，必须唯一且非空
    Phone VARCHAR(20) DEFAULT 'N/A',                      -- 手机号码，默认为'N/A'
    ProfileLink VARCHAR(100) DEFAULT 'N/A',               -- 个人信息网站链接，默认为'N/A'
    CreatedTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,      -- 记录创建时间，默认为当前时间
    UpdatedTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP -- 记录最后更新时间，默认为当前时间并在更新时自动修改
);

# 图片类型表 (保存网站支持的图片类型（普通图、病理图）)
CREATE TABLE ImageType (
    ImageTypeId INT AUTO_INCREMENT PRIMARY KEY,
    ImageTypeName VARCHAR(50) NOT NULL,
    ImageExtensions VARCHAR(500) NOT NULL
);
INSERT INTO ImageType (ImageTypeName, ImageExtensions)
VALUES
('病理图', '["mrxs", "tif"]');

# 数据集表 (现在的数据集表表达一个较大的概念，在编码时将其设定为和ImageGroup表的父目录，形成两层目录来确定一组图片)
CREATE TABLE Project (
	ProjectId INT AUTO_INCREMENT PRIMARY KEY,                -- 自增长的数据集ID
  	ProjectName VARCHAR(50) NOT NULL,                        -- 数据集名，非空
  	Description VARCHAR(2000) DEFAULT 'N/A',				 -- 数据集描述信息，默认为'N/A'
  	UserId INT NOT NULL,									 -- 关联的用户，外键（不在数据库中设计外键） 
  	ImageTypeId INT NOT NULL,								 -- 关联的图片类型，外键（不在数据库中设计外键）
  	CreatedTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,         -- 记录创建时间，默认为当前时间
    UpdatedTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 记录最后更新时间，默认为当前时间并在更新时自动修改
  	version INT NOT NULL DEFAULT 0                           -- 版本号，用于实现乐观锁
);

# 图片组表（ImageGroup表，保存图片的最小一级，是Project的子目录）
CREATE TABLE ImageGroup (
	ImageGroupId INT AUTO_INCREMENT PRIMARY KEY,               -- 自增长的图片组ID
    ImageGroupName VARCHAR(50) NOT NULL,                       -- 图片组名，非空
    Description VARCHAR(2000) NOT NULL DEFAULT 'N/A',          -- 图片组描述信息，非空，默认为'N/A'
    ProjectId INT NOT NULL,                                    -- 关联的数据集ID，外键（不在数据库中设计外键）
    CreatedTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,           -- 记录创建时间，默认为当前时间
    UpdatedTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 记录最后更新时间，默认为当前时间并在更新时自动修改
  	version INT NOT NULL DEFAULT 0                             -- 版本号，用于实现乐观锁
);

# 图片表 
CREATE TABLE Image (
    ImageId INT AUTO_INCREMENT PRIMARY KEY,                -- 自增长的图片ID
    ImageUrl VARCHAR(255) NOT NULL,                        -- 图片的URL，非空
    ImageName VARCHAR(255) NOT NULL,                       -- 图片名称
    ImageGroupId INT NOT NULL,                             -- 关联的图片组ID，外键（不在数据库中设计外键）
    ImageTypeId INT NOT NULL,                              -- 关联的图片类型ID，外键（不在数据库中设计外键）
    Status INT NOT NULL DEFAULT 0,                         -- 图片状态，默认为0 （0:未标注，1:已标注，2:标注完成）
    CreatedTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,       -- 记录创建时间，默认为当前时间
    UpdatedTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 记录最后更新时间，默认为当前时间并在更新时自动修改
  	version INT NOT NULL DEFAULT 0                         -- 版本号，用于实现乐观锁
);

# 大模型任务类型表
CREATE TABLE LLMTaskType (
    LLMTaskTypeId INT AUTO_INCREMENT PRIMARY KEY,                -- 自增长的大模型推理任务ID
    LLMTaskTypeName VARCHAR(255) NOT NULL, 	                     -- 任务名称，非空
  	isPreProcessTask INT NOT NULL,							 -- 是否为预处理任务
    Prompt VARCHAR(3000) NOT NULL, 								 -- 定义具体的prompt
    Description VARCHAR(3000) NOT NULL							 -- 任务描述，描述任务具体完成任务和对输入输出的简单描述
);

INSERT INTO LLMTaskType (LLMTaskTypeName, isPreProcessTask, Prompt, Description)
VALUES 
('实时问答任务', 0, 'N/A', '由用户输入决定'),
('病变区域分割任务', 1, 'Can you confirm if this pathology picture has cancerous areas? If it does, please indicate their edges.', '识别病理图中的癌症区域，给出癌变区域掩码，并分型分级给出类别标签。输出：1.类别标签 2.癌变区域掩码'),
('脉管癌栓检测任务', 1, 'Please identify and create bounding boxes around every blood vessel visible in this image, including both large and small vessels.', '识别并检测血管位置，并对癌细胞核进行检测和计数。输出：1.血管检测框 2.癌细胞核检测框和计数'),
('神经侵犯检测任务', 1, 'Please locate and highlight every nerve visible in this pathology image.', '识别并检测神经位置，判断血管是否有侵犯。输出：1.神经检测框 2.是否侵犯【二分类标签】'),
('淋巴结转移分割任务',1, 'Could you analyze and segment all the cancerous regions in the lymph node shown in this image?', '识别淋巴结区域，给出转移区域掩码，并给出是否转移的二分类标签。输出：1.淋巴结区域分割 2.是否转移【二分类标签】'),
('肝组织病变区域分割任务', 1, 'Please identify and segment the diagnostic area in this image.','识别周围肝组织，给出病变区域掩码，并给出是否病变的类别标签。输出：1.类别标签 2.病变区域掩码'),
('细胞核检测任务', 1, 'Find all epithelial cell nuclei and neoplastic cell nuclei in the pathology image and represent each with a bounding box, using [x1, y1, x2, y2] for coordinates scaled to a scale of 0 to 100 as integers.', '识别并检测细胞核位置，并给出细胞核类别。输出：1.类别标签 2.细胞核检测框');

CREATE INDEX idx_project_UserId ON project (UserId);
CREATE INDEX idx_imagegroup_ProjectId ON imagegroup (ProjectId);
CREATE INDEX idx_image_ImageGroupId ON image (ImageGroupId);
CREATE INDEX idx_image_Status ON image (Status);