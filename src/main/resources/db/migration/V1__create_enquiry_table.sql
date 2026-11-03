CREATE TABLE enquiry (
                         id UUID PRIMARY KEY,
                         reference VARCHAR(30) NOT NULL UNIQUE,
                         submission_key UUID NOT NULL UNIQUE,

                         customer_name VARCHAR(120) NOT NULL,
                         email VARCHAR(254),
                         phone VARCHAR(32),
                         preferred_contact VARCHAR(20) NOT NULL,

                         enquiry_type VARCHAR(30) NOT NULL,
                         vehicle_make VARCHAR(60),
                         vehicle_model VARCHAR(80),
                         vehicle_year SMALLINT,
                         registration VARCHAR(20),
                         message VARCHAR(4000) NOT NULL,

                         status VARCHAR(20) NOT NULL DEFAULT 'NEW',
                         privacy_notice_version VARCHAR(30) NOT NULL,

                         created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT chk_enquiry_name
                             CHECK (length(trim(customer_name)) > 0),

                         CONSTRAINT chk_enquiry_message
                             CHECK (length(trim(message)) > 0),

                         CONSTRAINT chk_enquiry_preferred_contact
                             CHECK (preferred_contact IN ('PHONE', 'EMAIL')),

                         CONSTRAINT chk_enquiry_contact_details
                             CHECK (
                                 (preferred_contact = 'PHONE'
                                     AND phone IS NOT NULL
                                     AND length(trim(phone)) > 0)
                                     OR
                                 (preferred_contact = 'EMAIL'
                                     AND email IS NOT NULL
                                     AND length(trim(email)) > 0)
                                 ),

                         CONSTRAINT chk_enquiry_type
                             CHECK (enquiry_type IN ('SERVICE', 'REPAIR', 'BUY', 'SELL', 'OTHER')),

                         CONSTRAINT chk_enquiry_status
                             CHECK (status IN ('NEW', 'CONTACTED', 'CLOSED', 'SPAM'))
);

CREATE INDEX idx_enquiry_status_created_at
    ON enquiry (status, created_at);