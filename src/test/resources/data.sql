INSERT INTO students (
  student_id, full_name, furigana, nickname,
  email, location, age, gender, remarks,
  is_deleted, deleted_at
) VALUES
(
  X'19D8186DB15E454E80EF4C8F4D550DD8',
  'Yamada Taro',
  'やまだ　たろう',
  'タロウ',
  'taro.yamada@example.com',
  'Osaka',
  35,
  'Male',
  'コース追加予定',
  0,
  NULL
),
(
  X'5A7441B8733A42898216CE772AC42B23',
  'Kwon Youngdon',
  'クォン・ヨンドン',
  'ドニー',
  'dony@example.com',
  '韓国',
  35,
  'Male',
  NULL,
  0,
  NULL
),
(
  X'71DF1C030775400D92B3B03EF4DE460F',
  'Kwon Youngdeuk',
  'クォン・ヨンドゥク',
  'ドゥキ',
  'deuki@example.com',
  '韓国',
  35,
  'Male',
  NULL,
  0,
  NULL
),
(
  X'74D5CC4EF9B411EF800D77A6DA541670',
  'Kim Jisoo',
  'キム ジス',
  'ジス',
  'jisoo@example.com',
  'Saitama',
  30,
  'Female',
  NULL,
  0,
  NULL
),
(
  X'74D64AB6F9B411EF800D77A6DA541670',
  'Kim Jennie',
  'キム ジェニ',
  'ジェニー',
  'jennie@example.com',
  'Kanagawa',
  29,
  'Female',
  NULL,
  0,
  NULL
),
(
  X'74D64D90F9B411EF800D77A6DA541670',
  'Park Chaeyoung',
  'パク チェヨン',
  'ロゼ',
  'rose@example.com',
  'Australia',
  28,
  'Female',
  NULL,
  0,
  NULL
),
(
  X'74D64EE4F9B411EF800D77A6DA541670',
  'Lalisa Manobal',
  'ラリサ マノバン',
  'リサ',
  'lisa@example.com',
  'Thailand',
  27,
  'Female',
  NULL,
  0,
  NULL
),
(
  X'839A5C6F3ACC45F3B92A46273A683215',
  'Park Hong-jun',
  'パク　ホンジュン',
  'テディ',
  'teddy@example.com',
  'ニューヨーク',
  46,
  'Male',
  NULL,
  0,
  NULL
),
(
  X'880C6AACF26111EF8A56CABD577D6BA1',
  'Choi Seunghyun',
  'チェ スンヒョン',
  'タプ',
  'top@example.com',
  'Hokkaido',
  37,
  'Male',
  NULL,
  0,
  NULL
),
(
  X'880CE32EF26111EF8A56CABD577D6BA1',
  'Dong Youngbae',
  'トン ヨンベ',
  'テヤン',
  'sol@example.com',
  'Tokyo',
  36,
  'Male',
  NULL,
  0,
  NULL
),
(
  X'880CE6C6F26111EF8A56CABD577D6BA1',
  'Kwon Jiyong',
  'クォン ジヨン',
  'ジヨン',
  'gd@example.com',
  'Nagoya',
  36,
  'Non-binary',
  NULL,
  0,
  NULL
),
(
  X'880CE838F26111EF8A56CABD577D6BA1',
  'Kang Daesung',
  'カン デソン',
  'デソン',
  'dlite@example.com',
  'Fukuoka',
  35,
  'Male',
  NULL,
  0,
  NULL
),
(
  X'880CF58AF26111EF8A56CABD577D6BA1',
  'Lee Seunghyun',
  'イ　スンヒョン',
  'スンリ',
  'vi@example.com',
  'Osaka',
  34,
  'Male',
  NULL,
  0,
  NULL
),
(
  X'8947FF9B034F46FF9C5F83AB1833A8FD',
  'Choi Dong Wook',
  'チェ　ドンウク',
  'セブン',
  'se7en@example.com',
  '韓国',
  40,
  'Male',
  NULL,
  1,
  '2025-04-20 20:31:51'
),
(
  X'D918FB5CFB4111EF8C1BE0591C80306E',
  'Yang Hyun-suk',
  'ヤン　ヒョンソク',
  'ヤンサ',
  'yang@example.com',
  'Chiba',
  55,
  'Male',
  NULL,
  1,
  '2025-04-20 20:30:42'
),
(
  X'DADAF78FC8C94725A0A6ED3BF31DD215',
  'Lee Bada',
  'イ　バダ',
  'バダ　リー',
  'bada@example.com',
  '韓国',
  29,
  'Female',
  NULL,
  0,
  NULL
);

INSERT INTO student_courses (
  course_id, student_id, course_name, start_date, end_date
) VALUES
(
  X'129E7E3CFED84173A5D0F62D589CC5E7',
  X'839A5C6F3ACC45F3B92A46273A683215',
  'デザインコース',
  DATE '2025-03-01',
  NULL
),
(
  X'1ADDAF706FEB460EA488C3DCD2D314CB',
  X'71DF1C030775400D92B3B03EF4DE460F',
  'デザインコース',
  DATE '2025-03-01',
  NULL
),
(
  X'22A76F780EC7423EBC35EE924EE82A0F',
  X'DADAF78FC8C94725A0A6ED3BF31DD215',
  'デザインコース',
  DATE '2025-03-01',
  NULL
),
(
  X'24619A3DBCD846E7826D4CFE067F6BAF',
  X'8947FF9B034F46FF9C5F83AB1833A8FD',
  'Javaコース',
  DATE '2024-04-01',
  DATE '2025-03-31'
),
(
  X'2A6E57D6F27311EF8A56CABD577D6BA1',
  X'880C6AACF26111EF8A56CABD577D6BA1',
  'Javaコース',
  DATE '2022-04-01',
  DATE '2023-05-31'
),
(
  X'2A6EF128F27311EF8A56CABD577D6BA1',
  X'880CE32EF26111EF8A56CABD577D6BA1',
  'AWSコース',
  DATE '2024-04-01',
  NULL
),
(
  X'2A6EF4CAF27311EF8A56CABD577D6BA1',
  X'880CE32EF26111EF8A56CABD577D6BA1',
  'Javaコース',
  DATE '2023-04-01',
  DATE '2024-03-31'
),
(
  X'2A6EF664F27311EF8A56CABD577D6BA1',
  X'880CE6C6F26111EF8A56CABD577D6BA1',
  '映像制作コース',
  DATE '2024-04-01',
  NULL
),
(
  X'2A6F105EF27311EF8A56CABD577D6BA1',
  X'880CE6C6F26111EF8A56CABD577D6BA1',
  'デザインコース',
  DATE '2023-04-01',
  DATE '2024-03-31'
),
(
  X'2A6F12FCF27311EF8A56CABD577D6BA1',
  X'880CE838F26111EF8A56CABD577D6BA1',
  '英会話コース',
  DATE '2023-04-01',
  NULL
),
(
  X'2A6F1432F27311EF8A56CABD577D6BA1',
  X'880CE838F26111EF8A56CABD577D6BA1',
  'WordPress副業コース',
  DATE '2022-04-01',
  DATE '2023-03-31'
),
(
  X'2A6F1554F27311EF8A56CABD577D6BA1',
  X'880CF58AF26111EF8A56CABD577D6BA1',
  'Webマーケティングコース',
  DATE '2022-04-01',
  DATE '2023-03-13'
),
(
  X'50AFE6D2140A4FFBAF147022E5550478',
  X'8947FF9B034F46FF9C5F83AB1833A8FD',
  'AWSコース',
  DATE '2025-04-01',
  NULL
),
(
  X'5A837B973D3D4043B16EF61533C685FA',
  X'19D8186DB15E454E80EF4C8F4D550DD8',
  'Javaコース',
  DATE '2024-04-01',
  DATE '2025-03-31'
),
(
  X'7F0D1D00F9B611EF800D77A6DA541670',
  X'74D64AB6F9B411EF800D77A6DA541670',
  'Webマーケティングコース',
  DATE '2022-04-01',
  NULL
),
(
  X'7F0D1F80F9B611EF800D77A6DA541670',
  X'74D64D90F9B411EF800D77A6DA541670',
  'フロントエンドコース',
  DATE '2023-04-01',
  NULL
),
(
  X'7F0D216AF9B611EF800D77A6DA541670',
  X'74D64EE4F9B411EF800D77A6DA541670',
  'フロントエンドコース',
  DATE '2023-04-01',
  NULL
),
(
  X'90153DDC1C2D11F0B106EBAA90D475CF',
  X'D918FB5CFB4111EF8C1BE0591C80306E',
  'Webマーケティングコース',
  DATE '2016-04-01',
  DATE '2019-05-31'
),
(
  X'9A80B054DE464C51B4034FDF68766211',
  X'5A7441B8733A42898216CE772AC42B23',
  '映像制作コース',
  DATE '2025-04-01',
  NULL
),
(
  X'9DC02CD6C5024DDCBB12CE64B6EF506A',
  X'74D5CC4EF9B411EF800D77A6DA541670',
  '映像制作コース',
  DATE '2024-04-01',
  NULL
),
(
  X'D55DE78B7FFD498393BBC9B2430C6270',
  X'19D8186DB15E454E80EF4C8F4D550DD8',
  'フロントエンドコース',
  DATE '2025-06-01',
  NULL
);
