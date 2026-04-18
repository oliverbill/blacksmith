-- ============================================================
-- Blacksmith local dev schema
-- ============================================================

-- Auth schema stub (mirrors Supabase auth.users for local dev)
CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE IF NOT EXISTS auth.users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email text
);

-- ============================================================
-- Enum types
-- ============================================================

CREATE TYPE agent_name AS ENUM ('CONSTITUTION', 'ARCHITECT', 'DEVELOPER');

CREATE TYPE artifact_type AS ENUM ('CONSTITUTION', 'IMPACT_ANALYSIS', 'CODE');

CREATE TYPE issue_type AS ENUM ('BUGFIX', 'TECH_DEBT', 'FEATURE');

CREATE TYPE run_status AS ENUM ('STARTED', 'DONE', 'CANCELLED', 'ERROR');

CREATE TYPE task_status AS ENUM (
    'DEV_PENDING', 'DEV_IN_PROGRESS', 'DEV_DONE', 'DEV_SKIPPED', 'DEV_FAILED',
    'TEST_PENDING', 'TEST_IN_PROGRESS', 'TEST_PASSED', 'TEST_FAILED', 'TEST_SKIPPED',
    'DEPLOY_PENDING', 'DEPLOY_IN_PROGRESS', 'DEPLOY_DONE', 'DEPLOY_FAILED'
);

CREATE TYPE refinement_status AS ENUM ('PENDING_CONFIRMATION', 'CONFIRMED', 'CANCELLED');

-- ============================================================
-- Application tables
-- ============================================================

CREATE TABLE public.tenants (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  timestamp with time zone NOT NULL DEFAULT now(),
    name        text NOT NULL,
    constitution_manual text,
    constitution_auto   text NOT NULL DEFAULT '',
    user_id     uuid NOT NULL UNIQUE,
    git_repos_urls text[] NOT NULL DEFAULT '{}',
    CONSTRAINT tenants_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id)
);

CREATE TABLE public.tenant_runs (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  timestamp with time zone NOT NULL DEFAULT now(),
    issue_type  issue_type NOT NULL,
    title       text NOT NULL,
    status      run_status NOT NULL,
    tenant_id   bigint NOT NULL,
    spec        text NOT NULL,
    full_sync_repo boolean DEFAULT false,
    CONSTRAINT tenant_runs_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenants(id)
);

CREATE TABLE public.run_artifacts (
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at        timestamp with time zone NOT NULL DEFAULT now(),
    run_id            bigint NOT NULL,
    agent             agent_name NOT NULL,
    content           text,
    type              artifact_type NOT NULL DEFAULT 'CONSTITUTION',
    source_artifact_id bigint,
    CONSTRAINT run_artifacts_run_id_fkey FOREIGN KEY (run_id) REFERENCES public.tenant_runs(id),
    CONSTRAINT run_artifacts_source_artifact_id_fkey FOREIGN KEY (source_artifact_id) REFERENCES public.run_artifacts(id)
);

CREATE TABLE public.task_executions (
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at        timestamp with time zone NOT NULL DEFAULT now(),
    planned_task_uuid uuid NOT NULL,
    artifact_id       bigint NOT NULL,
    status            task_status NOT NULL,
    llm_provider      text,
    CONSTRAINT task_executions_artifact_id_fkey FOREIGN KEY (artifact_id) REFERENCES public.run_artifacts(id)
);

CREATE TABLE public.refinement_requests (
    id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id          bigint,
    source_artifact_id bigint,
    feedback           text,
    created_at         timestamp without time zone DEFAULT now(),
    start_step         text,
    status             refinement_status DEFAULT 'PENDING_CONFIRMATION',
    refinement_result  text,
    CONSTRAINT refinement_requests_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenants(id),
    CONSTRAINT refinement_requests_source_artifact_id_fkey FOREIGN KEY (source_artifact_id) REFERENCES public.run_artifacts(id)
);

-- ============================================================
-- Spring Batch tables
-- (also created automatically by spring.batch.jdbc.initialize-schema=always,
--  included here for completeness / schema documentation)
-- ============================================================

CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_SEQ      MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_STEP_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;

CREATE TABLE IF NOT EXISTS public.batch_job_instance (
    job_instance_id bigint NOT NULL PRIMARY KEY,
    version         bigint,
    job_name        character varying(100) NOT NULL,
    job_key         character varying(32) NOT NULL,
    CONSTRAINT job_inst_un UNIQUE (job_name, job_key)
);

CREATE TABLE IF NOT EXISTS public.batch_job_execution (
    job_execution_id bigint NOT NULL PRIMARY KEY,
    version          bigint,
    job_instance_id  bigint NOT NULL,
    create_time      timestamp without time zone NOT NULL,
    start_time       timestamp without time zone,
    end_time         timestamp without time zone,
    status           character varying(10),
    exit_code        character varying(2500),
    exit_message     character varying(2500),
    last_updated     timestamp without time zone,
    CONSTRAINT job_inst_exec_fk FOREIGN KEY (job_instance_id) REFERENCES public.batch_job_instance(job_instance_id)
);

CREATE TABLE IF NOT EXISTS public.batch_job_execution_params (
    job_execution_id bigint NOT NULL,
    parameter_name   character varying(100) NOT NULL,
    parameter_type   character varying(100) NOT NULL,
    parameter_value  character varying(2500),
    identifying      character(1) NOT NULL,
    CONSTRAINT job_exec_params_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id)
);

CREATE TABLE IF NOT EXISTS public.batch_job_execution_context (
    job_execution_id  bigint NOT NULL PRIMARY KEY,
    short_context     character varying(2500) NOT NULL,
    serialized_context text,
    CONSTRAINT job_exec_ctx_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id)
);

CREATE TABLE IF NOT EXISTS public.batch_step_execution (
    step_execution_id  bigint NOT NULL PRIMARY KEY,
    version            bigint NOT NULL,
    step_name          character varying(100) NOT NULL,
    job_execution_id   bigint NOT NULL,
    create_time        timestamp without time zone NOT NULL,
    start_time         timestamp without time zone,
    end_time           timestamp without time zone,
    status             character varying(10),
    commit_count       bigint,
    read_count         bigint,
    filter_count       bigint,
    write_count        bigint,
    read_skip_count    bigint,
    write_skip_count   bigint,
    process_skip_count bigint,
    rollback_count     bigint,
    exit_code          character varying(2500),
    exit_message       character varying(2500),
    last_updated       timestamp without time zone,
    CONSTRAINT job_exec_step_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id)
);

CREATE TABLE IF NOT EXISTS public.batch_step_execution_context (
    step_execution_id  bigint NOT NULL PRIMARY KEY,
    short_context      character varying(2500) NOT NULL,
    serialized_context text,
    CONSTRAINT step_exec_ctx_fk FOREIGN KEY (step_execution_id) REFERENCES public.batch_step_execution(step_execution_id)
);
