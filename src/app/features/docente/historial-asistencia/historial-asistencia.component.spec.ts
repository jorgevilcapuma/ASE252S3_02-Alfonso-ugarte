import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HistorialAsistenciaComponent } from './historial-asistencia.component';

describe('HistorialAsistenciaComponent', () => {
  let component: HistorialAsistenciaComponent;
  let fixture: ComponentFixture<HistorialAsistenciaComponent>;

  beforeEach(async () => {
    localStorage.setItem('ugartino-modo', 'local');
    await TestBed.configureTestingModule({
      imports: [HistorialAsistenciaComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(HistorialAsistenciaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});