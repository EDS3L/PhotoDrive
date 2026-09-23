import raw from './answers.json?raw';
import type { StudyQuestion } from '../types';

export const questions: StudyQuestion[] = JSON.parse(raw);
