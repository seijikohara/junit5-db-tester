-- Comprehensive test DDL covering standard SQL data types compatible with Excel
-- Focuses on data type testing for Excel-based data insertion and verification
-- Excludes binary types (BLOB, BINARY) and database-specific dialect types

DROP TABLE IF EXISTS COMPREHENSIVE_DATA_TYPES;
CREATE TABLE COMPREHENSIVE_DATA_TYPES
(
    -- Primary key
    ID                    INT PRIMARY KEY,
    
    -- Integer types
    TINYINT_COL          TINYINT,
    SMALLINT_COL         SMALLINT,
    INT_COL              INT,
    INTEGER_COL          INTEGER,
    BIGINT_COL           BIGINT,
    
    -- Decimal/Numeric types
    DECIMAL_COL          DECIMAL(10,2),
    NUMERIC_COL          NUMERIC(8,3),
    DECIMAL_PRECISION    DECIMAL(15,4),
    
    -- Floating point types
    REAL_COL             REAL,
    FLOAT_COL            FLOAT,
    DOUBLE_COL           DOUBLE PRECISION,
    
    -- Character types
    CHAR_COL             CHAR(10),
    VARCHAR_COL          VARCHAR(100),
    VARCHAR_LONG         VARCHAR(500),
    TEXT_COL             TEXT,
    
    -- Date and time types
    DATE_COL             DATE,
    TIME_COL             TIME,
    DATETIME_COL         DATETIME,
    TIMESTAMP_COL        TIMESTAMP NULL DEFAULT NULL,
    
    -- Boolean type
    BOOLEAN_COL          BOOLEAN,
    
    -- Nullable variations
    NULLABLE_INT         INT NULL,
    NULLABLE_VARCHAR     VARCHAR(50) NULL,
    NULLABLE_DATE        DATE NULL,
    
    -- Default values (for testing framework's handling of defaults)
    DEFAULT_INT          INT DEFAULT 0,
    DEFAULT_VARCHAR      VARCHAR(20) DEFAULT 'default_value',
    DEFAULT_BOOLEAN      BOOLEAN DEFAULT FALSE,
    DEFAULT_TIMESTAMP    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);