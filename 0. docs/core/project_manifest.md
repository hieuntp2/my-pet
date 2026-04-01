# AI Pet Robot Project
Project Manifest / Knowledge Index

Version: v1
Owner: Hieu Le
Project Type: Hobby Robotics + AI Research
Architecture Style: Hybrid AI Robot (Android Brain + Microcontroller Body)

---

# 1. Project Vision

This project aims to build an **AI Pet Companion Robot** that can:

• recognize people  
• remember interactions  
• develop personality  
• interact naturally with humans  
• move in the physical world

The robot is designed as a **long-term evolving system**.

Instead of building everything at once, the system evolves through **three progressive phases**.

The key design philosophy:

> Emotion + behavior first. Intelligence second.

A robot that feels alive does not require perfect AI.

It requires:
- memory
- personality
- expressive behavior
- perception of people

---

# 2. Core Architecture Principle

The system follows a **three-layer architecture**.


Robot System

AI Brain (Android Phone)
↓
Robot Controller (Microcontroller)
↓
Physical World (Motors + Sensors)


Android acts as:

• AI brain  
• perception system  
• memory system  
• personality engine  
• conversation system  

Microcontroller acts as:

• hardware controller  
• motor control  
• sensor interface  
• safety layer  

---

# 3. Development Phases

The robot will evolve through **three phases**.

---

# Phase 1 — Android Pet Brain

Goal:

Build a **digital pet companion** that runs entirely on Android.

The phone becomes the **robot's brain and face**.

Capabilities:

• camera perception  
• face recognition  
• object recognition  
• long-term memory  
• event logging  
• personality development  
• avatar animation (robot face)

Key concept:

The robot exists as a **virtual pet** before it has a body.

Documents:

- 02_android_pet_brain_architecture.md
- 06_personality_engine.md
- 07_robot_memory_system.md

---

# Phase 2 — AI Intelligence Layer

Goal:

Enable the robot to **communicate and reason** using AI.

Capabilities:

• natural conversation
• question answering
• memory-aware conversation
• emotional tone generation
• suggestion of robot actions

Possible AI providers:

• OpenAI
• Google Gemini
• local models

Architecture:

LLM suggests actions.

Robot brain decides whether to execute them.

Document:

- 03_ai_cloud_integration.md

---

# Phase 3 — Physical Robot Body

Goal:

Give the AI pet a **physical body**.

Android phone is mounted on the robot.

The phone controls the robot through a microcontroller.

Capabilities:

• movement
• sensor awareness
• physical interaction
• exploration

Hardware responsibilities:

Microcontroller handles:

• motors
• servo control
• sensor reading
• safety fallback

Document:

- 04_robot_body_hardware.md

---

# 4. Overall System Architecture

The complete system architecture is described in:

05_full_system_architecture.md

Main components:

Android Brain
Memory System
Personality Engine
Behavior Engine
Perception System
Robot Controller
Sensors and Actuators
Cloud AI

---

# 5. Personality System

Personality is the core of the robot.

The robot should feel like a **living digital creature**.

Personality evolves through:

• interaction history
• emotional states
• reinforcement (positive / negative feedback)

The system maintains:

• personality traits
• mood states
• relationship scores with humans

Document:

06_personality_engine.md

---

# 6. Memory System

The robot has multiple types of memory.

Short-term memory  
Recent events and observations.

Long-term memory  
People, objects, locations.

Semantic memory  
Facts about the world.

Relationship memory  
History with specific people.

Document:

07_robot_memory_system.md

---

# 7. Perception

Perception primarily uses the **Android phone camera**.

Capabilities:

• face detection
• face recognition
• object recognition
• motion detection

Future sensors may include:

• depth sensors
• microphones
• ultrasonic sensors

Perception data feeds into:

Memory + Behavior System.

---

# 8. Behavior Engine

The behavior engine decides:

What the robot should do next.

Inputs:

• memory
• perception
• personality
• mood
• environment

Outputs:

• animation
• speech
• robot movement
• interaction

---

# 9. Inspiration from Existing Robots

The project research analyzes existing robots such as:

• Sony Aibo
• Loona
• EMO
• Vector
• LOVOT
• Amazon Astro
• Eilik

These robots show that **believability comes from behavior and emotion**, not raw AI power.

Document:

01_robot_pet_market_research.md

---

# 10. Long-Term Evolution

Future versions of the robot may include:

• home exploration
• autonomous navigation
• advanced robotics arms
• multi-room mapping
• multi-user relationship modeling
• multi-robot communication

---

# 11. Development Strategy

This project follows an **iterative hobby development approach**.

Start simple.

Add complexity gradually.

Priority order:

1. Personality
2. Memory
3. Perception
4. Behavior
5. Intelligence
6. Robotics

---

# 12. Repository Structure

Recommended project structure:


pet-robot/

docs/
01_robot_pet_market_research.md
02_android_pet_brain_architecture.md
03_ai_cloud_integration.md
04_robot_body_hardware.md
05_full_system_architecture.md
06_personality_engine.md
07_robot_memory_system.md
project_manifest.md

android-brain/
robot-body/
ai-services/
simulation/


---

# 13. Guiding Philosophy

A successful robot companion is not defined by intelligence.

It is defined by **believability**.

The robot should feel:

• curious  
• playful  
• attentive  
• responsive  

If users feel the robot **has a personality**, the project succeeds.

---

# End of Manifest