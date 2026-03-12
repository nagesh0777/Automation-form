export type PipelineStatus = 'COMPLETE' | 'IN_PROGRESS' | 'NOT_STARTED' | 'BLOCKED'
export type WorkItemStatus = 'DONE' | 'IN_PROGRESS' | 'TODO' | 'BLOCKED'
export type BlockerType = 'BLOCKER' | 'DEPENDENCY'

export interface SprintBurndown {
  totalStories?: number
  completed?: number
  inProgress?: number
  remainingSp?: number
  idealSp?: number
  variance?: number
  trend?: string
}

export interface DoraMetrics {
  deploymentFrequencyToday?: number
  deploymentFrequencySprintAvg?: number
  deploymentFrequencyTarget?: number
  leadTimeToday?: number
  leadTimeAvg?: number
  leadTimeTarget?: number
  changeFailureRateToday?: number
  changeFailureRateAvg?: number
  changeFailureRateTarget?: number
  mttrToday?: number
  mttrAvg?: number
  mttrTarget?: number
}

export interface PipelineTracker {
  itemId: string
  dataPrep?: PipelineStatus
  modelDev?: PipelineStatus
  integration?: PipelineStatus
  validation?: PipelineStatus
  deployment?: PipelineStatus
  notes?: string
}

export interface WorkItem {
  memberName: string
  storyId?: string
  taskDescription?: string
  status?: WorkItemStatus
  ageDays?: number
  flag?: string
}

export interface BlockerDependency {
  itemId?: string
  type?: BlockerType
  description?: string
  owner?: string
  actionNeeded?: string
}

export interface RiskObservation {
  description?: string
}

export interface FocusOutlook {
  todayFocus?: string
  tomorrowOutlook?: string
  decisionsNeeded?: string
}

export interface ReportRequest {
  podName: string
  date: string
  sprintNumber: number
  dayOfSprint: number
  goal?: string
  scrumMasterName?: string
  managerName?: string
  sprintBurndown?: SprintBurndown
  doraMetrics?: DoraMetrics
  pipelineTrackers: PipelineTracker[]
  workItems: WorkItem[]
  blockersDependencies: BlockerDependency[]
  risksObservations: RiskObservation[]
  focusOutlook?: FocusOutlook
}

export interface ReportResponse extends ReportRequest {
  id: number
  versionNumber: number
  warningText?: string
  createdBy: string
  createdAt: string
}

export interface AuthResponse {
  token: string
  email: string
  name: string
  role: 'SCRUM_MASTER' | 'TEAM_MEMBER' | 'MANAGER'
}
