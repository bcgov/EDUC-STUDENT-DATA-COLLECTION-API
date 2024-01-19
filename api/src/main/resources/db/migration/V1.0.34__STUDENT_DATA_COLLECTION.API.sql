UPDATE ENROLLED_PROGRAM_CODE
SET LABEL = 'Programme Francophone',
    DESCRIPTION = 'Programme Francophone'
WHERE ENROLLED_PROGRAM_CODE = '05';

UPDATE ENROLLED_PROGRAM_CODE
SET LABEL = 'Core French',
    DESCRIPTION = 'Core French'
WHERE ENROLLED_PROGRAM_CODE = '08';

UPDATE ENROLLED_PROGRAM_CODE
SET LABEL = 'Early French Immersion',
    DESCRIPTION = 'Early French Immersion'
WHERE ENROLLED_PROGRAM_CODE = '11';

UPDATE ENROLLED_PROGRAM_CODE
SET LABEL = 'Late French Immersion',
    DESCRIPTION = 'Late French Immersion'
WHERE ENROLLED_PROGRAM_CODE = '14';

UPDATE ENROLLED_PROGRAM_CODE
SET LABEL = 'English Language Learning',
    DESCRIPTION = 'English Language Learning'
WHERE ENROLLED_PROGRAM_CODE = '17';

UPDATE ENROLLED_PROGRAM_CODE
SET LABEL = 'Indigenous Language Culture',
    DESCRIPTION = 'Indigenous Language and Culture'
WHERE ENROLLED_PROGRAM_CODE = '29';

UPDATE ENROLLED_PROGRAM_CODE
SET LABEL = 'Indigenous Support Services',
    DESCRIPTION = 'Indigenous Support Services'
WHERE ENROLLED_PROGRAM_CODE = '33';

UPDATE ENROLLED_PROGRAM_CODE
SET LABEL = 'Other Appr Indigenous Programs',
    DESCRIPTION = 'Other Approved Indigenous Programs'
WHERE ENROLLED_PROGRAM_CODE = '36';

UPDATE ENROLLED_PROGRAM_CODE
SET LABEL = 'Career Preparation',
    DESCRIPTION = 'Career Preparation'
WHERE ENROLLED_PROGRAM_CODE = '40';

UPDATE ENROLLED_PROGRAM_CODE
SET LABEL = 'Co-operative Education',
    DESCRIPTION = 'Co-operative Education'
WHERE ENROLLED_PROGRAM_CODE = '41';

UPDATE ENROLLED_PROGRAM_CODE
SET LABEL = 'Youth WORK in Trades Program',
    DESCRIPTION = 'Youth WORK in Trades Program'
WHERE ENROLLED_PROGRAM_CODE = '42';

UPDATE ENROLLED_PROGRAM_CODE
SET LABEL = 'Career Tech Youth TRAIN Trades',
    DESCRIPTION = 'Career Technical or Youth TRAIN in Trades'
WHERE ENROLLED_PROGRAM_CODE = '43';

UPDATE CAREER_PROGRAM_CODE
SET LABEL = 'Business & Applied Business',
    DESCRIPTION = 'Business & Applied Business'
WHERE CAREER_PROGRAM_CODE = 'XA';

UPDATE CAREER_PROGRAM_CODE
SET LABEL = 'Fine Arts, Design, & Media',
    DESCRIPTION = 'Fine Arts, Design, & Media'
WHERE CAREER_PROGRAM_CODE = 'XB';

UPDATE CAREER_PROGRAM_CODE
SET LABEL = 'Fitness & Recreation',
    DESCRIPTION = 'Fitness & Recreation'
WHERE CAREER_PROGRAM_CODE = 'XC';

UPDATE CAREER_PROGRAM_CODE
SET LABEL = 'Health & Human Services',
    DESCRIPTION = 'Health & Human Services'
WHERE CAREER_PROGRAM_CODE = 'XD';

UPDATE CAREER_PROGRAM_CODE
SET LABEL = 'Liberal Arts & Humanities',
    DESCRIPTION = 'Liberal Arts & Humanities'
WHERE CAREER_PROGRAM_CODE = 'XE';

UPDATE CAREER_PROGRAM_CODE
SET LABEL = 'Science & Applied Science',
    DESCRIPTION = 'Science & Applied Science'
WHERE CAREER_PROGRAM_CODE = 'XF';

UPDATE CAREER_PROGRAM_CODE
SET LABEL = 'Tourism, Hospitality, & Foods',
    DESCRIPTION = 'Tourism, Hospitality, & Foods'
WHERE CAREER_PROGRAM_CODE = 'XG';

UPDATE CAREER_PROGRAM_CODE
SET LABEL = 'Trades & Technology',
    DESCRIPTION = 'Trades & Technology'
WHERE CAREER_PROGRAM_CODE = 'XH';

UPDATE SPECIAL_EDUCATION_CATEGORY_CODE
SET LABEL = 'Physically Dependent',
    DESCRIPTION = 'Physically Dependent'
WHERE SPECIAL_EDUCATION_CATEGORY_CODE = 'A';

UPDATE SPECIAL_EDUCATION_CATEGORY_CODE
SET LABEL = 'Deafblind',
    DESCRIPTION = 'Deafblind'
WHERE SPECIAL_EDUCATION_CATEGORY_CODE = 'B';

UPDATE SPECIAL_EDUCATION_CATEGORY_CODE
SET LABEL = 'Moderate to Profound Int Disab',
    DESCRIPTION = 'Moderate to Profound Intellectual Disability'
WHERE SPECIAL_EDUCATION_CATEGORY_CODE = 'C';

UPDATE SPECIAL_EDUCATION_CATEGORY_CODE
SET LABEL = 'Phys Disab or Chronic Impair',
    DESCRIPTION = 'Physical Disability or Chronic Health Impairment'
WHERE SPECIAL_EDUCATION_CATEGORY_CODE = 'D';

UPDATE SPECIAL_EDUCATION_CATEGORY_CODE
SET LABEL = 'Visual Impairment',
    DESCRIPTION = 'Visual Impairment'
WHERE SPECIAL_EDUCATION_CATEGORY_CODE = 'E';

UPDATE SPECIAL_EDUCATION_CATEGORY_CODE
SET LABEL = 'Trades & Technology',
    DESCRIPTION = 'Trades & Technology'
WHERE SPECIAL_EDUCATION_CATEGORY_CODE = 'XH';

UPDATE SPECIAL_EDUCATION_CATEGORY_CODE
SET LABEL = 'Deaf or Hard of Hearing',
    DESCRIPTION = 'Deaf or Hard of Hearing'
WHERE SPECIAL_EDUCATION_CATEGORY_CODE = 'F';

UPDATE SPECIAL_EDUCATION_CATEGORY_CODE
SET LABEL = 'Autism Spectrum Disorder',
    DESCRIPTION = 'Autism Spectrum Disorder'
WHERE SPECIAL_EDUCATION_CATEGORY_CODE = 'G';

UPDATE SPECIAL_EDUCATION_CATEGORY_CODE
SET LABEL = 'Serious Mental/Behav Illness',
    DESCRIPTION = 'Intensive Behaviour Intervention/Serious Mental Illness'
WHERE SPECIAL_EDUCATION_CATEGORY_CODE = 'H';

UPDATE SPECIAL_EDUCATION_CATEGORY_CODE
SET LABEL = 'Mild Intellectual Disability',
    DESCRIPTION = 'Mild Intellectual Disability'
WHERE SPECIAL_EDUCATION_CATEGORY_CODE = 'K';

UPDATE SPECIAL_EDUCATION_CATEGORY_CODE
SET LABEL = 'Gifted',
    DESCRIPTION = 'Gifted'
WHERE SPECIAL_EDUCATION_CATEGORY_CODE = 'P';

UPDATE SPECIAL_EDUCATION_CATEGORY_CODE
SET LABEL = 'Learning Disability',
    DESCRIPTION = 'Learning Disability'
WHERE SPECIAL_EDUCATION_CATEGORY_CODE = 'Q';

UPDATE SPECIAL_EDUCATION_CATEGORY_CODE
SET LABEL = 'Moderate Mental/Behav Illness',
    DESCRIPTION = 'Moderate Behaviour Support/Mental Illness'
WHERE SPECIAL_EDUCATION_CATEGORY_CODE = 'R';

UPDATE SCHOOL_FUNDING_CODE
SET LABEL = 'Out Province/Internat Student',
    DESCRIPTION = 'Out-of-Province/International Student'
WHERE SCHOOL_FUNDING_CODE = '14';

UPDATE SCHOOL_FUNDING_CODE
SET LABEL = 'Newcomer Refugee',
    DESCRIPTION = 'Newcomer Refugee'
WHERE SCHOOL_FUNDING_CODE = '16';

UPDATE SCHOOL_FUNDING_CODE
SET LABEL = 'Ordinarily Living on Reserve',
    DESCRIPTION = 'Ordinarily Living on Reserve'
WHERE SCHOOL_FUNDING_CODE = '20';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = INITCAP(LABEL),
    DESCRIPTION = INITCAP(DESCRIPTION);

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Indo Iranian (Other)',
    DESCRIPTION = 'Indo Iranian (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '047';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Montagnais-Naskapi',
    DESCRIPTION = 'Montagnais-Naskapi'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '056';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Wakashanes (Other)',
    DESCRIPTION = 'Wakashanes (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '070';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Aboriginal (Other)',
    DESCRIPTION = 'Aboriginal (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '071';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Tagalog (Philipino)',
    DESCRIPTION = 'Tagalog (Philipino)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '086';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Malayo-Poly (Other)',
    DESCRIPTION = 'Malayo-Poly (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '087';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Nigero-Congo (Other)',
    DESCRIPTION = 'Nigero-Congo (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '089';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Sino-Tibetan (Other)',
    DESCRIPTION = 'Sino-Tibetan (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '169';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Austro-Asiatic (Other)',
    DESCRIPTION = 'Austro-Asiatic (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '170';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Altai-Turkic (Other)',
    DESCRIPTION = 'Altai-Turkic (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '171';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Australian Languages',
    DESCRIPTION = 'Australian Languages'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '186';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Baltic (Other)',
    DESCRIPTION = 'Baltic (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '192';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Bamileke Languages',
    DESCRIPTION = 'Bamileke Languages'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '193';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Central American (Other)',
    DESCRIPTION = 'Central American (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '203';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Chamic Languages',
    DESCRIPTION = 'Chamic Languages'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '205';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'South Eastern Slavic',
    DESCRIPTION = 'South Eastern Slavic'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '212';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Classical Newari',
    DESCRIPTION = 'Classical Newari'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '215';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Cushitic (Other)',
    DESCRIPTION = 'Cushitic (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '219';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Finno-Ugrian (Other)',
    DESCRIPTION = 'Finno-Ugrian (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '228';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Sami Languages (Other)',
    DESCRIPTION = 'Sami Languages (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '250';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Indo-European (Other)',
    DESCRIPTION = 'Indo-European (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '251';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Interlingua-Intl Aux Lng Assoc',
    DESCRIPTION = 'Interlingua-Intl Aux Lng Assoc'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '252';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Khoisan (Other)',
    DESCRIPTION = 'Khoisan (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '263';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'Papuan (Other)',
    DESCRIPTION = 'Papuan (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '306';

UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET LABEL = 'South American Indian (Other)',
    DESCRIPTION = 'South American Indian (Other)'
WHERE HOME_LANGUAGE_SPOKEN_CODE = '319';