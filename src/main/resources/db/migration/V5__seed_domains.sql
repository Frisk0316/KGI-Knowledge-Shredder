INSERT INTO trainers (trainer_id, display_name, role_name)
VALUES ('trainer_001', 'Development Trainer', 'TRAINER')
ON CONFLICT (trainer_id) DO NOTHING;

INSERT INTO knowledge_domains (domain_id, domain_name, description) VALUES
    (1, 'LifeInsurance', 'Life insurance products, policy structure, beneficiaries, claims, and coverage discussions.'),
    (2, 'InvestmentLinked', 'Investment-linked products, funds, asset allocation, and risk-return concepts.'),
    (3, 'CRM', 'Client relationship management, service follow-up, and communication quality.'),
    (4, 'Compliance', 'Financial compliance, AML/KYC checks, disclosures, and operating controls.'),
    (5, 'WealthManagement', 'Wealth planning, succession, trust topics, and broader asset management decisions.'),
    (6, 'TaxRegulations', 'Tax rules, filing requirements, withholding, and tax planning considerations.'),
    (7, 'Other', 'Use when the material does not fit the predefined financial knowledge domains.')
ON CONFLICT (domain_id) DO UPDATE
    SET domain_name = EXCLUDED.domain_name,
        description = EXCLUDED.description;
