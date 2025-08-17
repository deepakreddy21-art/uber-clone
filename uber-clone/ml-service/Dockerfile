# Multi-stage build for ML service
FROM python:3.11-slim AS base

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    git \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy requirements first for better caching
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Create models directory
RUN mkdir -p /app/models

# Create non-root user
RUN useradd --create-home --shell /bin/bash appuser && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# Run the application
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]

# GPU-enabled version (uncomment for GPU support)
# FROM nvidia/cuda:11.8-runtime-ubuntu20.04 AS gpu-base
# 
# # Install Python and system dependencies
# RUN apt-get update && apt-get install -y \
#     python3.11 \
#     python3.11-pip \
#     python3.11-dev \
#     build-essential \
#     curl \
#     git \
#     && rm -rf /var/lib/apt/lists/*
# 
# # Set working directory
# WORKDIR /app
# 
# # Copy requirements
# COPY requirements.txt .
# 
# # Install GPU-enabled versions
# RUN pip3.11 install --no-cache-dir -r requirements.txt
# 
# # Copy application code
# COPY . .
# 
# # Create models directory
# RUN mkdir -p /app/models
# 
# # Create non-root user
# RUN useradd --create-home --shell /bin/bash appuser && \
#     chown -R appuser:appuser /app
# 
# # Switch to non-root user
# USER appuser
# 
# # Expose port
# EXPOSE 8000
# 
# # Health check
# HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
#     CMD curl -f http://localhost:8000/health || exit 1
# 
# # Run with GPU support
# CMD ["python3.11", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
